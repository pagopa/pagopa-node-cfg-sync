package it.gov.pagopa.node.cfgsync.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.stereotype.Component;

@Component
public class ViewNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalCatalogName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalColumnName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalSchemaName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalSequenceName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return identifier;
    }

    @Override
    public Identifier toPhysicalTableName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
        return convertToSnakeCase(identifier);
    }

    private Identifier convertToSnakeCase(final Identifier identifier) {
        if(identifier.getText().equals("{cdi_preferences_target}")){
            String cdiPreferencesTable = System.getenv("CDI_PREFERENCES_TABLE");
            return Identifier.toIdentifier(cdiPreferencesTable);
        }
        if(identifier.getText().equals("{elenco_servizi_target}")){
            String elencoServiziTable = System.getenv("ELENCO_SERVIZI_TABLE");
            return Identifier.toIdentifier(elencoServiziTable);
        }
        return identifier;
    }
}

