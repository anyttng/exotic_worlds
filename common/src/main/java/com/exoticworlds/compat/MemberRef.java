package com.exoticworlds.compat;

record MemberRef(String owner, String name, String descriptor) {
    private static final String ANY_QUANTIFIER = "*";
    private static final String PLUS_QUANTIFIER = "+";
    private static final char QUANTIFIER_OPEN = '{';

    static MemberRef parse(String reference) {
        String owner = null;
        String rest = reference;
        int semicolon = rest.indexOf(';');
        int paren = rest.indexOf('(');
        if (rest.startsWith("L") && semicolon > 0 && (paren < 0 || semicolon < paren)) {
            owner = rest.substring(1, semicolon);
            rest = rest.substring(semicolon + 1);
        }

        int colon = rest.indexOf(':');
        paren = rest.indexOf('(');
        if (colon >= 0 && (paren < 0 || colon < paren)) {
            return new MemberRef(owner, withoutQuantifier(rest.substring(0, colon)), rest.substring(colon + 1));
        }

        if (paren >= 0) {
            return new MemberRef(owner, withoutQuantifier(rest.substring(0, paren)), rest.substring(paren));
        }

        return new MemberRef(owner, withoutQuantifier(rest), null);
    }

    boolean matches(String owner, String name, String descriptor) {
        return this.name.equals(name)
                && (this.owner == null || this.owner.equals(owner))
                && (this.descriptor == null || this.descriptor.equals(descriptor));
    }

    private static String withoutQuantifier(String name) {
        int brace = name.indexOf(QUANTIFIER_OPEN);
        if (brace >= 0) {
            return name.substring(0, brace);
        }

        return name.endsWith(ANY_QUANTIFIER) || name.endsWith(PLUS_QUANTIFIER)
                ? name.substring(0, name.length() - 1)
                : name;
    }
}
