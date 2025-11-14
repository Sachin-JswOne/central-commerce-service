package com.jswone.commerce.core.service;

import com.commercetools.api.client.ApiRoot;
import com.commercetools.api.defaultconfig.ApiFactory;
import com.commercetools.api.defaultconfig.ServiceRegion;
import com.commercetools.http.okhttp4.CtOkHttp4Client;
import com.commercetools.importapi.defaultconfig.ImportApiFactory;
import io.vrap.rmf.base.client.ApiHttpClient;
import io.vrap.rmf.base.client.AuthenticationToken;
import io.vrap.rmf.base.client.oauth2.AnonymousSessionTokenSupplier;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import io.vrap.rmf.base.client.oauth2.ClientCredentialsTokenSupplier;
import io.vrap.rmf.base.client.oauth2.StaticTokenSupplier;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Log4j2
@Service
public class ClientService {

    public static ApiRoot apiRoot;
    public static ApiHttpClient importHttpClient;

    // TODO: Add the Constant-Token Client

    /**
     * @return
     * @throws IOException
     */
    public static com.commercetools.importapi.client.ApiRoot createImportApiClient(
            final String prefix) throws IOException {

        final Properties prop = new Properties();
        prop.load(ClientService.class.getResourceAsStream("/application.properties"));
        String clientId = prop.getProperty(prefix + "clientId");
        String clientSecret = prop.getProperty(prefix + "clientSecret");

        importHttpClient =
                ImportApiFactory.defaultClient(
                        ClientCredentials.of()
                                .withClientId(clientId)
                                .withClientSecret(clientSecret)
                                .build(),
                        com.commercetools.importapi.defaultconfig.ServiceRegion.GCP_US_CENTRAL1
                                .getOAuthTokenUrl(),
                        com.commercetools.importapi.defaultconfig.ServiceRegion.GCP_US_CENTRAL1
                                .getApiUrl());

        return ImportApiFactory.create(() -> importHttpClient);
    }

    public ApiRoot createConstantTokenApiClient(String token, String apiUrl) throws IOException {

        final ApiHttpClient apiHttpClient = ClientFactory.createStatic(token, apiUrl);

        return ApiFactory.create(() -> apiHttpClient);
    }

    public static AuthenticationToken getTokenForClientCredentialsFlow(final String prefix)
            throws IOException {

        final Properties prop = new Properties();
        prop.load(ClientService.class.getResourceAsStream("/application.properties"));
        String projectKey = prop.getProperty(prefix + "projectKey");
        String clientId = prop.getProperty(prefix + "clientId");
        String clientSecret = prop.getProperty(prefix + "clientSecret");
        AuthenticationToken token = null;
        try (final ClientCredentialsTokenSupplier clientCredentialsTokenSupplier =
                     new ClientCredentialsTokenSupplier(
                             clientId,
                             clientSecret,
                             null,
                             ServiceRegion.GCP_US_CENTRAL1.getOAuthTokenUrl(),
                             new CtOkHttp4Client())) {
            token = clientCredentialsTokenSupplier.getToken().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(" error while fetching client token : {}", e);
        }
        return token;
    }

    static class ClientFactory {
        public static ApiHttpClient createStatic(final String token, final String apiEndpoint) {
            AuthenticationToken t = new AuthenticationToken();
            t.setAccessToken(token);
            return io.vrap.rmf.base.client.ClientFactory.create(
                    apiEndpoint, new CtOkHttp4Client(), new StaticTokenSupplier(t));
        }

        public static ApiHttpClient createAnonFlow(
                final ClientCredentials credentials,
                final String tokenEndpoint,
                final String apiEndpoint) {
            return io.vrap.rmf.base.client.ClientFactory.create(
                    apiEndpoint,
                    new CtOkHttp4Client(),
                    new AnonymousSessionTokenSupplier(
                            credentials.getClientId(),
                            credentials.getClientSecret(),
                            credentials.getScopes(),
                            tokenEndpoint,
                            new CtOkHttp4Client()));
        }
    }
}

