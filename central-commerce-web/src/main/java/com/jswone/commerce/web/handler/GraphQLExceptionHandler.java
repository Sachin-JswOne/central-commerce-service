package com.jswone.commerce.web.handler;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

        if (ex instanceof CentralCommerceServiceException ccse) {

            log.warn("GraphQL business exception with path={}, message={}", env.getExecutionStepInfo().getPath(),
                    ccse.getMessage());

            return graphql.GraphqlErrorBuilder.newError(env)
                    .message(ccse.getMessage())
                    .errorType(mapErrorType(ccse.getHttpStatus()))
                    .extensions(Map.of("httpStatus", ccse.getHttpStatus().value()))
                    .build();
        }

        log.error("Unhandled GraphQL exception with path={}", env.getExecutionStepInfo().getPath(), ex);

        return graphql.GraphqlErrorBuilder.newError(env)
                .message("Internal server error")
                .errorType(ErrorType.INTERNAL_ERROR)
                .extensions(Map.of("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value() ))
                .build();
    }

    private ErrorType mapErrorType(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorType.BAD_REQUEST;
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }
}
