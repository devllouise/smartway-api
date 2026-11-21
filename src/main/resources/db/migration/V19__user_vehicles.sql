-- Veiculo do usuario (carro ou moto) para calcular o custo real da viagem
CREATE TABLE user_vehicles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_type VARCHAR(20) NOT NULL,            -- CAR ou MOTORCYCLE
    fipe_brand_code VARCHAR(20),
    brand_name VARCHAR(100),
    fipe_model_code VARCHAR(20),
    model_name VARCHAR(200),
    fipe_year_code VARCHAR(20),
    model_year INTEGER,
    fuel VARCHAR(30),                              -- Gasolina, Alcool, Flex, Diesel
    km_per_liter DOUBLE PRECISION NOT NULL,
    km_per_liter_source VARCHAR(20) NOT NULL,      -- USER, INMETRO ou DEFAULT
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, vehicle_type)
);

-- Tabela do PBE Veicular (Inmetro), importada de resources/data/pbev.csv
CREATE TABLE vehicle_consumption (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(150) NOT NULL,
    version VARCHAR(250),
    model_year INTEGER,
    fuel VARCHAR(30),
    city_km_per_liter DOUBLE PRECISION,
    highway_km_per_liter DOUBLE PRECISION,
    source VARCHAR(60) NOT NULL DEFAULT 'INMETRO PBEV'
);
CREATE INDEX idx_vehicle_consumption_brand ON vehicle_consumption (brand);
