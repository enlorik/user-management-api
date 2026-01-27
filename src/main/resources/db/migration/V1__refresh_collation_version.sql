-- Refresh PostgreSQL collation version to fix version mismatch warnings
-- This addresses the issue where the database collation version doesn't match the system version
-- Railway Issue: Database created with collation 2.36 but system provides 2.41

DO $$
BEGIN
    -- Refresh collation version for the current database
    EXECUTE 'ALTER DATABASE ' || quote_ident(current_database()) || ' REFRESH COLLATION VERSION';
    
    -- Log success
    RAISE NOTICE 'Successfully refreshed collation version for database: %', current_database();
EXCEPTION
    WHEN OTHERS THEN
        -- Log the error but don't fail the migration
        RAISE WARNING 'Could not refresh collation version: %', SQLERRM;
END $$;
