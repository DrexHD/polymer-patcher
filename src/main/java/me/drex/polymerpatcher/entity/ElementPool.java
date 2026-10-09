package me.drex.polymerpatcher.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ElementPool<T> {
    private final List<T> elements = new ArrayList<>();
    private final Supplier<T> factory;
    private final Consumer<T> add;
    private final Consumer<T> remove;
    private int used;

    ElementPool(Supplier<T> factory, Consumer<T> add, Consumer<T> remove) {
        this.factory = factory;
        this.add = add;
        this.remove = remove;
    }

    void beginFrame() {
        used = 0;
    }

    void next(BiConsumer<T, Boolean> update) {
        boolean created = used == elements.size();
        T element = created ? factory.get() : elements.get(used);
        update.accept(element, created);
        if (created) {
            add.accept(element);
            elements.add(element);
        }
        used++;
    }

    void endFrame(Consumer<T> update) {
        while (elements.size() > used) {
            remove.accept(elements.removeLast());
        }
        elements.forEach(update);
    }
}
