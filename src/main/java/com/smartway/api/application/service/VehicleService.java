package com.smartway.api.application.service;

import com.smartway.api.adapter.out.fipe.FipeAdapter;
import com.smartway.api.adapter.out.persistence.UserVehicleRepository;
import com.smartway.api.adapter.out.persistence.VehicleConsumptionRepository;
import com.smartway.api.domain.entity.UserVehicle;
import com.smartway.api.domain.entity.VehicleConsumption;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

/**
 * Veiculo do usuario: modelo pela FIPE, consumo estimado pelo Inmetro (PBE Veicular)
 * e km/l editavel. Calcula o custo de combustivel da viagem.
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final FipeAdapter fipe;
    private final UserVehicleRepository vehicles;
    private final VehicleConsumptionRepository consumption;

    @Value("${smartway.vehicle.fuel-price.gasoline:6.30}") private double gasolinePrice;
    @Value("${smartway.vehicle.fuel-price.ethanol:4.40}") private double ethanolPrice;
    @Value("${smartway.vehicle.fuel-price.diesel:6.10}") private double dieselPrice;
    @Value("${smartway.vehicle.default-km-per-liter.car:11.0}") private double defaultCar;
    @Value("${smartway.vehicle.default-km-per-liter.motorcycle:35.0}") private double defaultMoto;

    public record VehicleRequest(String vehicleType, String brandCode, String brandName,
                                 String modelCode, String modelName, String yearCode,
                                 Integer modelYear, String fuel, Double kmPerLiter) {}

    // ---------- catalogo (FIPE) ----------
    public List<Map<String, Object>> brands(String type) { return fipe.brands(type); }
    public List<Map<String, Object>> models(String type, String brand) { return fipe.models(type, brand); }
    public List<Map<String, Object>> years(String type, String brand, String model) { return fipe.years(type, brand, model); }

    /**
     * Consumo estimado a partir dos codigos da FIPE (os mesmos que o app recebeu nas listas).
     * O back busca na FIPE o nome, o ano e o combustivel e procura o consumo no Inmetro.
     */
    public Map<String, Object> estimateByCodes(String type, String brandCode, String modelCode, String yearCode) {
        Map<String, Object> d = fipe.detail(type, brandCode, modelCode, yearCode);
        String brand = str(d.get("brand")), model = str(d.get("model")), fuel = str(d.get("fuel"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("brandName", brand);
        out.put("modelName", model);
        out.put("modelYear", d.get("modelYear"));
        out.put("fuel", fuel);
        out.putAll(estimate(type, brand, model, fuel));
        return out;
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    /** Consumo estimado para um modelo, para o app preencher o campo de km/l. */
    public Map<String, Object> estimate(String type, String brandName, String modelName, String fuel) {
        Optional<VehicleConsumption> match = findConsumption(brandName, modelName, fuel);
        Map<String, Object> out = new LinkedHashMap<>();
        if (match.isPresent()) {
            out.put("kmPerLiter", round1(cityKmL(match.get(), fuel)));
            out.put("source", "INMETRO");
            out.put("matchedVersion", match.get().getModel() + " " + Objects.toString(match.get().getVersion(), "").trim());
        } else {
            out.put("kmPerLiter", defaultFor(type));
            out.put("source", "DEFAULT");
            out.put("matchedVersion", null);
        }
        return out;
    }

    // ---------- veiculo do usuario ----------
    public List<Map<String, Object>> list(Long userId) {
        return vehicles.findByUserId(userId).stream().map(this::toMap).toList();
    }

    @Transactional
    public Map<String, Object> save(Long userId, VehicleRequest r) {
        String type = normalizeType(r.vehicleType());
        UserVehicle v = vehicles.findByUserIdAndVehicleType(userId, type)
            .orElseGet(() -> UserVehicle.builder().userId(userId).vehicleType(type).build());
        // Com os codigos da FIPE, nome, ano e combustivel sao preenchidos pelo back
        if (r.brandCode() != null && r.modelCode() != null && r.yearCode() != null
                && (r.modelName() == null || r.brandName() == null)) {
            Map<String, Object> d = fipe.detail(type, r.brandCode(), r.modelCode(), r.yearCode());
            r = new VehicleRequest(type, r.brandCode(), r.brandName() != null ? r.brandName() : str(d.get("brand")),
                r.modelCode(), str(d.get("model")), r.yearCode(),
                r.modelYear() != null ? r.modelYear() : (d.get("modelYear") instanceof Number n ? n.intValue() : null),
                r.fuel() != null ? r.fuel() : str(d.get("fuel")), r.kmPerLiter());
        }
        v.setFipeBrandCode(r.brandCode());
        v.setBrandName(r.brandName());
        v.setFipeModelCode(r.modelCode());
        v.setModelName(r.modelName());
        v.setFipeYearCode(r.yearCode());
        v.setModelYear(r.modelYear());
        v.setFuel(r.fuel());
        if (r.kmPerLiter() != null && r.kmPerLiter() > 0) {
            if (r.kmPerLiter() > 80) throw new IllegalStateException("Consumo invalido: informe um valor ate 80 km/l");
            v.setKmPerLiter(r.kmPerLiter());
            v.setKmPerLiterSource("USER");
        } else {
            Map<String, Object> e = estimate(type, r.brandName(), r.modelName(), r.fuel());
            v.setKmPerLiter(((Number) e.get("kmPerLiter")).doubleValue());
            v.setKmPerLiterSource((String) e.get("source"));
        }
        return toMap(vehicles.save(v));
    }

    @Transactional
    public void delete(Long userId, String type) {
        vehicles.findByUserIdAndVehicleType(userId, normalizeType(type)).ifPresent(vehicles::delete);
    }

    // ---------- custo usado pelo motor de recomendacao ----------
    /** Custo de combustivel em reais para a distancia. Sem veiculo cadastrado, usa o consumo padrao. */
    public double fuelCost(Long userId, String type, double distanceKm) {
        String t = normalizeType(type);
        Optional<UserVehicle> v = userId == null ? Optional.empty() : vehicles.findByUserIdAndVehicleType(userId, t);
        double kmL = v.map(UserVehicle::getKmPerLiter).filter(x -> x > 0).orElse(defaultFor(t));
        double price = fuelPrice(v.map(UserVehicle::getFuel).orElse(null));
        return distanceKm / kmL * price;
    }

    // ---------- auxiliares ----------
    private Optional<VehicleConsumption> findConsumption(String brandName, String modelName, String fuel) {
        if (brandName == null || modelName == null) return Optional.empty();
        String brand = brandKey(brandName);
        String model = norm(modelName);
        List<VehicleConsumption> candidates = consumption.findAll().stream()
            .filter(c -> brandKey(c.getBrand()).equals(brand)).toList();
        VehicleConsumption best = null; int bestScore = 0;
        for (VehicleConsumption c : candidates) {
            String name = norm(c.getModel());
            if (!model.startsWith(name) && !model.contains(" " + name)) continue;
            int score = name.length() * 10 + overlap(model, norm(Objects.toString(c.getVersion(), "")));
            if (fuel != null && c.getFuel() != null && norm(c.getFuel()).contains(norm(fuel))) score += 5;
            if (score > bestScore) { bestScore = score; best = c; }
        }
        return Optional.ofNullable(best);
    }

    private double cityKmL(VehicleConsumption c, String fuel) {
        Double v = c.getCityKmPerLiter() != null ? c.getCityKmPerLiter() : c.getHighwayKmPerLiter();
        return v != null && v > 0 ? v : defaultCar;
    }

    private int overlap(String a, String b) {
        Set<String> wa = new HashSet<>(Arrays.asList(a.split(" ")));
        int n = 0; for (String w : b.split(" ")) if (w.length() > 1 && wa.contains(w)) n++;
        return n;
    }

    private double fuelPrice(String fuel) {
        String f = fuel == null ? "" : norm(fuel);
        if (f.contains("alcool") || f.contains("etanol")) return ethanolPrice;
        if (f.contains("diesel")) return dieselPrice;
        return gasolinePrice; // gasolina, flex ou nao informado
    }

    private double defaultFor(String type) { return "MOTORCYCLE".equals(normalizeType(type)) ? defaultMoto : defaultCar; }

    private String normalizeType(String t) { return "MOTORCYCLE".equalsIgnoreCase(t) ? "MOTORCYCLE" : "CAR"; }

    private String brandKey(String b) {
        String n = norm(b);
        if (n.contains("chevrolet")) return "chevrolet";
        if (n.startsWith("vw") || n.contains("volkswagen")) return "volkswagen";
        if (n.contains("chery")) return "chery";
        return n.split(" ")[0];
    }

    private static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase().replaceAll("[^a-z0-9.]+", " ").trim();
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private Map<String, Object> toMap(UserVehicle v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vehicleType", v.getVehicleType());
        m.put("brandCode", v.getFipeBrandCode());
        m.put("brandName", v.getBrandName());
        m.put("modelCode", v.getFipeModelCode());
        m.put("modelName", v.getModelName());
        m.put("yearCode", v.getFipeYearCode());
        m.put("modelYear", v.getModelYear());
        m.put("fuel", v.getFuel());
        m.put("kmPerLiter", v.getKmPerLiter());
        m.put("kmPerLiterSource", v.getKmPerLiterSource());
        m.put("fuelPrice", fuelPrice(v.getFuel()));
        return m;
    }
}
