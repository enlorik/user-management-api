-- Refresh database collation version to resolve collation mismatch warnings
-- This is primarily for PostgreSQL databases deployed on Railway
-- The script includes error handling to prevent failures in test environments

DO $$
BEGIN
    -- Attempt to refresh collation version
    -- This operation requires database-level privileges and may not work in all environments
    EXECUTE 'ALTER DATABASE ' || quote_ident(current_database()) || ' REFRESH COLLATION VERSION';
    RAISE NOTICE 'Successfully refreshed collation version for database %', current_database();
EXCEPTION
    WHEN insufficient_privilege THEN
        -- User doesn't have permission to alter database (common in test/H2 environments)
        RAISE WARNING 'Insufficient privileges to refresh collation version: %', SQLERRM;
    WHEN feature_not_supported THEN
        -- Database doesn't support this operation (e.g., H2, some PostgreSQL versions)
        RAISE WARNING 'REFRESH COLLATION VERSION not supported by this database: %', SQLERRM;
    WHEN syntax_error THEN
        -- Syntax not recognized (e.g., H2 database)
        RAISE WARNING 'Database does not support REFRESH COLLATION VERSION syntax: %', SQLERRM;
    WHEN OTHERS THEN
        -- Catch any other errors and log as warning instead of failing migration
        RAISE WARNING 'Could not refresh collation version: %', SQLERRM;
END $$;
