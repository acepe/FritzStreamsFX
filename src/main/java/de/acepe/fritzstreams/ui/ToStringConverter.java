package de.acepe.fritzstreams.ui;

import java.util.function.Function;

import javafx.util.StringConverter;

public class ToStringConverter<T> extends StringConverter<T> {

    private final Function<T, String> toStringFunction;

    public ToStringConverter(Function<T, String> toStringFunction) {
        this.toStringFunction = toStringFunction;
    }

    @Override
    public String toString(T object) {
        return toStringFunction.apply(object);
    }

    @Override
    public T fromString(String string) {
        return null;
    }
}
