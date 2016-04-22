package com.jetbrains.teamsys.dnq.database;

import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityId;
import jetbrains.exodus.entitystore.EntityIterator;
import jetbrains.exodus.database.TransientStoreSession;
import jetbrains.exodus.entitystore.iterate.EntityIteratorWithPropId;
import org.jetbrains.annotations.NotNull;

class PersistentEntityIteratorWithPropIdWrapper implements EntityIteratorWithPropId {

    @NotNull
    protected final EntityIteratorWithPropId source;
    private final TransientStoreSession session;

    PersistentEntityIteratorWithPropIdWrapper(@NotNull final EntityIteratorWithPropId source, final TransientStoreSession session) {
        this.source = source;
        this.session = session;
    }

    public boolean hasNext() {
        return source.hasNext();
    }

    public Entity next() {
        //TODO: do not save in session?
        final Entity persistentEntity = source.next();
        if (persistentEntity == null) {
            return null;
        }
        return session.newEntity(persistentEntity);
    }

    public String currentLinkName() {
        return source.currentLinkName();
    }

    public void remove() {
        source.remove();
    }

    public EntityId nextId() {
        return source.nextId();
    }

    public boolean dispose() {
        return source.dispose();
    }

    public boolean skip(int number) {
        return source.skip(number);
    }

    public boolean shouldBeDisposed() {
        return source.shouldBeDisposed();  //TODO: revisit EntityIterator interface and remove these stub method
    }
}
