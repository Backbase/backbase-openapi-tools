package com.backbase.oss.boat.transformers;

import static java.lang.String.format;
import static org.apache.commons.beanutils.PropertyUtils.getPropertyDescriptor;
import static org.apache.commons.beanutils.PropertyUtils.getSimpleProperty;
import static org.apache.commons.beanutils.PropertyUtils.setSimpleProperty;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import lombok.NonNull;
import org.apache.commons.beanutils.expression.DefaultResolver;
import org.apache.commons.beanutils.expression.Resolver;

public final class PropUtils {
    static private final Resolver RESOLVER = new DefaultResolver();

    private boolean create;
    private boolean lenient;

    public PropUtils create() {
        this.create = true;

        return this;
    }

    public PropUtils lenient() {
        this.lenient = true;

        return this;
    }

    public <X> X get(Object bean, String path) {
        while (bean != null && RESOLVER.hasNested(path)) {
            bean = getProperty(bean, RESOLVER.next(path));
            path = RESOLVER.remove(path);
        }

        return bean != null ? (X) getProperty(bean, path) : null;
    }

    public <X> PropUtils ifPresent(Object bean, String path, Consumer<X> action) {
        final X x = get(bean, path);

        if (x != null) {
            action.accept(x);
        }

        return this;
    }

    private Object getProperty(@NonNull Object bean, String name) {
        Object child;

        try {
            child = getSimpleProperty(bean, name);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(format("Cannot read property %s.%s ",
                bean.getClass().getSimpleName(), name), e);
        } catch (final NoSuchMethodException e) {
            if (this.lenient) {
                return null;
            }

            throw new RuntimeException(format("Cannot read property %s.%s ",
                bean.getClass().getSimpleName(), name), e);
        }

        if (child != null || !this.create) {
            return child;
        }

        PropertyDescriptor desc;

        try {
            desc = getPropertyDescriptor(bean, name);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(format("Cannot read descriptor %s.%s ",
                bean.getClass().getSimpleName(), name), e);
        }

        try {
            child = desc.getPropertyType().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(format("Cannot create property %s.%s ",
                bean.getClass().getSimpleName(), name), e);
        }

        try {
            setSimpleProperty(bean, name, child);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(format("Cannot write property %s.%s ",
                bean.getClass().getSimpleName(), name), e);
        }

        return child;
    }
}


