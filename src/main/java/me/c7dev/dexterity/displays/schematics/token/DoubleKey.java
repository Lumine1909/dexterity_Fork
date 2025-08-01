package me.c7dev.dexterity.displays.schematics.token;

import me.c7dev.dexterity.displays.schematics.token.Token.TokenType;

public class DoubleKey {

    private final double val;
    private final TokenType type;

    public DoubleKey(TokenType type, double val) {
        this.val = val;
        this.type = type;
    }

    public boolean equals(Object o) {
        if (!(o instanceof DoubleKey k)) {
            return false;
        }
        return k.getValue() == val && k.getType() == type;
    }

    public int hashCode() {
        return (int) val ^ type.hashCode();
    }

    public double getValue() {
        return val;
    }

    public TokenType getType() {
        return type;
    }

}
