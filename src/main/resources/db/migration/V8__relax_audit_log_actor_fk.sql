-- Permite que los logs de auditoría registren operaciones realizadas tanto por
-- usuarios institucionales (users), usuarios maestros de BankOS (master_users)
-- como procesos de sistema, sin violar la llave foránea sobre users(id).
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_actor_user_id_fkey;
