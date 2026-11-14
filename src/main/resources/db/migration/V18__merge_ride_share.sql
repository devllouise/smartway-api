-- Uber e 99 viram uma unica opcao: "Carro de aplicativo" (RIDE_SHARE)
UPDATE user_preferences SET available_modes = REPLACE(available_modes, 'NINETY_NINE', 'RIDE_SHARE') WHERE available_modes LIKE '%NINETY_NINE%';
UPDATE user_preferences SET available_modes = REPLACE(available_modes, 'UBER', 'RIDE_SHARE') WHERE available_modes LIKE '%UBER%';
UPDATE user_preferences SET available_modes = REPLACE(available_modes, 'RIDE_SHARE,RIDE_SHARE', 'RIDE_SHARE') WHERE available_modes LIKE '%RIDE_SHARE,RIDE_SHARE%';
UPDATE route_feedbacks SET chosen_mode = 'RIDE_SHARE' WHERE chosen_mode IN ('UBER', 'NINETY_NINE');
UPDATE route_history SET recommended_mode = 'RIDE_SHARE' WHERE recommended_mode IN ('UBER', 'NINETY_NINE');
UPDATE route_history SET card_json = REPLACE(REPLACE(card_json, '"UBER"', '"RIDE_SHARE"'), '"NINETY_NINE"', '"RIDE_SHARE"');
