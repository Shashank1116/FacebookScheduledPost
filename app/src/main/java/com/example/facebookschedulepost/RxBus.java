package com.example.facebookschedulepost;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

/**
 * Created by shubham on 19-03-2018.
 **/

public class RxBus {

    private RxBus(){

    }

    private static RxBus instance;


    public static  RxBus getInstance(){
        if(instance == null){
            createInstance();
        }
        return instance;
    }

    private static synchronized void createInstance(){
        if(instance == null){
            instance = new RxBus();
        }
    }
    public Relay<Object> get_bus() {
        return _bus;
    }

    private final Relay<Object> _bus = PublishRelay.create().toSerialized();

    public void send(Object o) {
        _bus.accept(o);
    }

    public Flowable<Object> asFlowable() {
        return _bus.toFlowable(BackpressureStrategy.LATEST);
    }

    public boolean hasObservers() {
        return _bus.hasObservers();
    }

    public void triggerEvent(Object object){
        if(hasObservers()) {
            send(object);
        }
    }
}
