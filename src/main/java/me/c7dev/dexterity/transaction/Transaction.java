package me.c7dev.dexterity.transaction;

import me.c7dev.dexterity.displays.DexterityDisplay;

public interface Transaction {

    DexterityDisplay undo();

    void redo();

    boolean isCommitted();

    boolean isUndone();

    boolean isPossible();

}
