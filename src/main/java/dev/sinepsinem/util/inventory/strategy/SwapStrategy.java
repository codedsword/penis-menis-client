package dev.sinepsinem.util.inventory.strategy;

import dev.sinepsinem.util.inventory.Result;

public interface SwapStrategy {
    boolean swap(Result result);

    boolean swapBack(int last, Result result);
}
