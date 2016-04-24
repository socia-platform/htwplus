# --- !Ups

ALTER TABLE group_ DROP COLUMN rootfolder_id;
ALTER TABLE group_ RENAME COLUMN mediafolder_id TO rootfolder_id;