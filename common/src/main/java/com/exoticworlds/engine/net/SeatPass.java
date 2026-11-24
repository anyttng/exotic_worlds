package com.exoticworlds.engine.net;

final class SeatPass {
    private static final int VIEW_SEAT = 0;

    private static final int ONE_SEAT = 1;

    private int answered = VIEW_SEAT;

    private int seats = ONE_SEAT;

    int getAnswered() {
        return answered;
    }

    void answer(int seat) {
        answered = seat;
    }

    int getSeats() {
        return seats;
    }

    void sawSeats(int count) {
        seats = Math.max(seats, count);
    }

    void reset() {
        answered = VIEW_SEAT;
        seats = ONE_SEAT;
    }
}
