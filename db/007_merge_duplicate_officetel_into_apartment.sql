-- Merge OFFICETEL properties into matching APARTMENT properties.
-- Match rule: same sido, sigungu, legal_dong, name, and jibun_address.
-- The script is idempotent: after matching OFFICETEL rows are deleted, reruns do nothing.

CREATE TEMPORARY TABLE property_merge_map AS
SELECT
    offi.property_id AS offi_property_id,
    apt.property_id AS apt_property_id
FROM properties offi
INNER JOIN properties apt
    ON apt.property_type = 'APARTMENT'
   AND offi.property_type = 'OFFICETEL'
   AND apt.sido = offi.sido
   AND apt.sigungu = offi.sigungu
   AND apt.legal_dong = offi.legal_dong
   AND apt.name = offi.name
   AND apt.jibun_address = offi.jibun_address;

ALTER TABLE property_merge_map
    ADD PRIMARY KEY (offi_property_id),
    ADD INDEX idx_property_merge_map_apt (apt_property_id);

START TRANSACTION;

UPDATE property_transactions t
INNER JOIN property_merge_map m
    ON m.offi_property_id = t.property_id
SET t.property_id = m.apt_property_id;

DELETE f
FROM favorite_apartments f
INNER JOIN property_merge_map m
    ON m.offi_property_id = f.property_id
INNER JOIN favorite_apartments existing_f
    ON existing_f.user_id = f.user_id
   AND existing_f.property_id = m.apt_property_id;

UPDATE favorite_apartments f
INNER JOIN property_merge_map m
    ON m.offi_property_id = f.property_id
SET f.property_id = m.apt_property_id;

UPDATE apartment_price_predictions pp
INNER JOIN property_merge_map m
    ON m.offi_property_id = pp.property_id
SET pp.property_id = m.apt_property_id;

UPDATE apartment_shap_values sv
INNER JOIN property_merge_map m
    ON m.offi_property_id = sv.property_id
SET sv.property_id = m.apt_property_id;

DELETE p
FROM properties p
INNER JOIN property_merge_map m
    ON m.offi_property_id = p.property_id;

DROP TEMPORARY TABLE property_merge_map;

COMMIT;
