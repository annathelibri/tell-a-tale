package net.notjustanna.libs.tellatale.loader

class LoaderException(override val message: String, override val cause: Throwable) : Exception(message, cause)