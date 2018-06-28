/*
 *  This file is part of Discord4J.
 *
 * Discord4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discord4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */

package discord4j.store.dsl.jvm;

import discord4j.store.dsl.LogicalStatement;
import discord4j.store.dsl.Property;
import discord4j.store.util.WithinRangePredicate;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Predicate;

public class SimpleLogicalStatement<T, V> implements LogicalStatement<T> {

    @Nullable
    private final Property<T> property;
    @Nullable
    private final MethodHandle handle;
    private final boolean matchRange;
    private final Predicate<T> tester;

    public SimpleLogicalStatement(Class<T> holder, Property<T> property, @Nullable V value) {
        this.property = property;
        try {
            this.handle = MethodHandles.lookup().findGetter(holder, property.getName(), property.getType());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        this.matchRange = false;
        this.tester = (t) -> {
            try {
                return t == null ? value == null : Objects.equals(handle.bindTo(t).invokeExact(), value);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };
    }

    public SimpleLogicalStatement(Class<T> holder, Property<T> property, V start, V end) {
        this.property = property;
        try {
            this.handle = MethodHandles.lookup().findGetter(holder, property.getName(), property.getType());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        this.matchRange = true;

        if (!(start instanceof Comparable || end instanceof Comparable))
            throw new RuntimeException("This constructor overload requires a comparable type!");

        this.tester = new WithinRangePredicate((Comparable<V>) start, (Comparable<V>) end);
    }

    private SimpleLogicalStatement(Predicate<T> override) {
        this.tester = override;
        this.matchRange = false;
        this.handle = null;
        this.property = null;
    }

    @Override
    public LogicalStatement<T> not() {
        return new SimpleLogicalStatement<T, V>(t -> !this.test(t));
    }

    @Override
    public LogicalStatement<T> and(LogicalStatement<T>... others) {
        return new SimpleLogicalStatement<T, V>(t -> {
            if (!this.test(t))
                return false;
            for (LogicalStatement<T> stmt : others) {
                if (!(stmt instanceof SimpleLogicalStatement))
                    throw new RuntimeException("Incompatible statements used!");
                if (!((SimpleLogicalStatement<T, V>) stmt).test(t))
                    return false;
            }
            return true;
        });
    }

    @Override
    public LogicalStatement<T> or(LogicalStatement<T>... others) {
        return new SimpleLogicalStatement<T, V>(t -> {
            if (this.test(t))
                return true;
            for (LogicalStatement<T> stmt : others) {
                if (!(stmt instanceof SimpleLogicalStatement))
                    throw new RuntimeException("Incompatible statements used!");
                if (((SimpleLogicalStatement<T, V>) stmt).test(t))
                    return true;
            }
            return false;
        });
    }

    @Override
    public LogicalStatement<T> xor(LogicalStatement<T>... others) {
        return new SimpleLogicalStatement<T, V>(t -> {
            boolean currValue = this.test(t);
            for (LogicalStatement<T> stmt : others) {
                if (!(stmt instanceof SimpleLogicalStatement))
                    throw new RuntimeException("Incompatible statements used!");
                if (((SimpleLogicalStatement<T, V>) stmt).test(t)) {
                    if (currValue)
                        return false;
                    currValue = true;
                }
            }
            return currValue;
        });
    }

    public boolean test(T obj) {
        return tester.test(obj);
    }
}
