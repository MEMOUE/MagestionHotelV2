package com.mghbackend.exception;

/**
 * Exception métier volontaire (ex: "Hôtel non trouvé", "Email déjà utilisé").
 * Son message est destiné à être affiché tel quel au client.
 * À ne jamais utiliser pour des erreurs techniques (DB, réseau, etc.).
 */
public class BusinessException extends RuntimeException {

	public BusinessException(String message) {
		super(message);
	}

	public BusinessException(String message, Throwable cause) {
		super(message, cause);
	}
}
