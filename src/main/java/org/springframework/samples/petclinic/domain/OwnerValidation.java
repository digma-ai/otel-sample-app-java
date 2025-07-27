package org.springframework.samples.petclinic.domain;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.samples.petclinic.owner.Owner;public class OwnerValidation {

    private static final int TIMEOUT_SECONDS = 30;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_RESET_TIME = 60000; // 1 minute

    private final ConcurrentHashMap<String, ValidationResult> validationCache = new ConcurrentHashMap<>();
    private int failureCount = 0;
    private boolean circuitOpen = false;
    private long lastFailureTime = 0;

    private final UserValidationService usrValSvc;
    private final PasswordUtils pwdUtils;
    private final Tracer otelTracer;
    private final RoleService roleSvc;
    private final TwoFactorAuthenticationService twoFASvc;

    public OwnerValidation(Tracer otelTracer) {
        this.pwdUtils = new PasswordUtils();
        this.roleSvc = new RoleService();
        this.otelTracer = otelTracer;
        this.usrValSvc = new UserValidationService();
        this.twoFASvc = new TwoFactorAuthenticationService();
    }

    @WithSpan
    public void ValidateOwnerWithExternalService(Owner owner) throws ValidationException {
        try {
            if (circuitOpen) {
                if (System.currentTimeMillis() - lastFailureTime > CIRCUIT_BREAKER_RESET_TIME) {
                    circuitOpen = false;
                    failureCount = 0;
                } else {
                    throw new CircuitBreakerException("Circuit breaker is open");
                }
            }

            String cacheKey = generateCacheKey(owner);
            ValidationResult cachedResult = validationCache.get(cacheKey);
            if (cachedResult != null && !cachedResult.isExpired()) {
                return;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                this.AuthServiceValidateUser(owner);
            });

            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            validationCache.put(cacheKey, new ValidationResult());
            failureCount = 0;
        } catch (TimeoutException e) {
            handleFailure();
            throw new ValidationTimeoutException("Validation service timeout", e);
        } catch (Exception e) {
            handleFailure();
            throw new ValidationException("Validation failed", e);
        }
    }

    @WithSpan
    public boolean ValidateUserAccess(String usr, String pswd, String sysCode) throws ValidationException {
        try {
            boolean vldUsr = usrValSvc.vldtUsr(usr);
            if (!vldUsr) {
                return false;
            }

            boolean vldPswd = pwdUtils.vldtPswd(usr, pswd);
            if (!vldPswd) {
                return false;
            }

            return true;
        } catch (Exception e) {
            throw new ValidationException("User access validation failed", e);
        }
    }

    private void handleFailure() {
        failureCount++;
        if (failureCount >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitOpen = true;
            lastFailureTime = System.currentTimeMillis();
        }
    }

    private String generateCacheKey(Owner owner) {
        return owner.getId() + "_" + owner.getVersion();
    }
}private static final int MAX_RETRY_ATTEMPTS = 3;
private static final long TWO_FA_TIMEOUT = 5000; // 5 seconds timeout
private static final long AUTH_SERVICE_TIMEOUT = 3000; // 3 seconds timeout

boolean vldUsrRole = false;
try {
    vldUsrRole = CompletableFuture.supplyAsync(() -> roleSvc.vldtUsrRole(usr, sysCode))
        .get(AUTH_SERVICE_TIMEOUT, TimeUnit.MILLISECONDS);
} catch (TimeoutException e) {
    throw new ServiceTimeoutException("Role validation timed out");
} catch (Exception e) {
    throw new ValidationException("Role validation failed", e);
}

if (!vldUsrRole) {
    return false;
}

boolean is2FASuccess = false;
try {
    is2FASuccess = CompletableFuture.supplyAsync(() -> twoFASvc.init2FA(usr))
        .get(TWO_FA_TIMEOUT, TimeUnit.MILLISECONDS);
} catch (TimeoutException e) {
    throw new ServiceTimeoutException("2FA initialization timed out");
} catch (Exception e) {
    throw new ValidationException("2FA initialization failed", e);
}

if (!is2FASuccess) {
    return false;
}

boolean is2FATokenValid = false;
int retry = 0;
while (retry < MAX_RETRY_ATTEMPTS && !is2FATokenValid) {
    try {
        String token = twoFASvc.getTokenInput();
        is2FATokenValid = CompletableFuture.supplyAsync(() -> twoFASvc.vldtToken(usr, token))
            .get(TWO_FA_TIMEOUT, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
        logger.warn("2FA token validation attempt " + (retry + 1) + " timed out");
    } catch (Exception e) {
        logger.error("2FA token validation attempt " + (retry + 1) + " failed", e);
    }
    retry++;
}

if (!is2FATokenValid) {
    return false;
}

return true;
}

@WithSpan
private void AuthServiceValidateUser(Owner owner) {
    try {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Authentication service interrupted", e);
            }
        }).get(AUTH_SERVICE_TIMEOUT, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
        throw new ServiceTimeoutException("Authentication service timed out");
    } catch (Exception e) {
        throw new ValidationException("Authentication service failed", e);
    }
}

@WithSpan
public boolean checkOwnerValidity(Owner owner) {
    return ValidateOwnerUser(owner);
}

private boolean ValidateOwnerUser(Owner owner) {
    Span span = otelTracer.spanBuilder("db_access_01").startSpan();try {
			for (int i = 0; i < 100; i++) {
				ValidateOwner();
			}
		}
		finally {
			span.end();
		}
		return true;
	}

	private void ValidateOwner() {
		Span span = otelTracer.spanBuilder("query_users_by_id")
			.setSpanKind(SpanKind.CLIENT)
			.setAttribute("db.system", "other_sql")
			.setAttribute("db.statement", "select * from users where id = :id")
			.startSpan();

		try {
			if (!isCircuitBreakerOpen()) {
				CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
					try {
						// delay(1);
					} catch (Exception e) {
						increaseFailureCount();
						throw new ValidationServiceException("External service validation failed", e);
					}
				});
				future.get(TIMEOUT_DURATION, TimeUnit.MILLISECONDS);
			} else {
				throw new CircuitBreakerOpenException("Circuit breaker is open");
			}
		} catch (TimeoutException e) {
			increaseFailureCount();
			throw new ValidationTimeoutException("Validation timed out", e);
		} catch (Exception e) {
			increaseFailureCount();
			throw new ValidationServiceException("Validation failed", e);
		} finally {
			span.end();
		}
	}

	public void PerformValidationFlow(Owner owner) {
//		if (owner.getPet("Jerry").isNew()) {
//			ValidateOwner();
//		}
	}

}