package marabillas.loremar.karrypdf.exceptions

class NoReferenceResolverException : Exception(
    "ReferenceResolver is not set. Call ObjectIdentifier.setReferenceResolver()" +
            " with an existing ReferenceResolver."
)