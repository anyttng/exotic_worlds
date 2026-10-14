package com.exoticworlds.compat.eminus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

public final class CopyBuilds<H> {
    public enum Offer {
        QUEUE, JOINED
    }

    public record Member<H>(long key, long request, float priority, @Nullable H held, int references) {
    }

    private static final class Group<H> {
        private final long storedKey;
        private final List<Member<H>> followers = new ArrayList<>();
        private Member<H> leader;

        private Group(long storedKey, Member<H> leader) {
            this.storedKey = storedKey;
            this.leader = leader;
        }
    }

    private final Map<Long, Group<H>> waiting = new HashMap<>();
    private final Map<Long, Group<H>> byLeader = new HashMap<>();
    private final Set<Long> absorbed = new HashSet<>();

    public synchronized Offer offer(long storedKey, Member<H> member) {
        Group<H> group = this.waiting.get(storedKey);
        if (group == null) {
            group = new Group<>(storedKey, member);
            this.waiting.put(storedKey, group);
            this.byLeader.put(member.request(), group);
            return Offer.QUEUE;
        }

        if (member.priority() <= group.leader.priority()) {
            group.followers.add(member);
            return Offer.JOINED;
        }

        this.byLeader.remove(group.leader.request());
        this.absorbed.add(group.leader.request());
        group.followers.add(group.leader);
        group.leader = member;
        this.byLeader.put(member.request(), group);
        return Offer.QUEUE;
    }

    public synchronized boolean skip(long request) {
        return this.absorbed.remove(request);
    }

    public synchronized void taken(long request) {
        Group<H> group = this.byLeader.get(request);
        if (group != null) {
            this.waiting.remove(group.storedKey, group);
        }
    }

    public synchronized List<Member<H>> delivered(long request) {
        Group<H> group = this.byLeader.remove(request);
        if (group == null) {
            return List.of();
        }

        this.waiting.remove(group.storedKey, group);
        return List.copyOf(group.followers);
    }

    public synchronized void clear() {
        this.waiting.clear();
        this.byLeader.clear();
        this.absorbed.clear();
    }
}
