package com.example.a1716342.beaconslocalisation;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class RSSIAverage implements Queue<ArrayList<Integer>> {

    //List of the ten last prediction (index of the classes)
    private LinkedList<ArrayList<Integer>> queue;

    //List of the number of occurrences for each class
    private int[] listOccur;

    //desired size for the
    private int size;

    public RSSIAverage(int size){
        this.queue = new LinkedList<ArrayList<Integer>>();
        this.size = size;
    }

    public ArrayList<Double> RSSIMean(){
        ArrayList<Double> mean = new ArrayList<>(4);
        mean.add(0.0);
        mean.add(0.0);
        mean.add(0.0);
        mean.add(0.0);

        for (ArrayList<Integer> listRSSI : queue){
            for (int i = 0; i<4; i++){
                mean.set(i,mean.get(i)+listRSSI.get(i));
            }
        }
        for (int i = 0; i<4; i++){
            mean.set(i, mean.get(i)/queue.size());
        }
        return mean;
    }

    public String toString(){
        String s = "rssi's :\n";
        for (ArrayList<Integer> listRSSI : queue){
            s+=listRSSI.toString()+"\n";
        }
        return s;
    }
    public void addLast(ArrayList<Integer> e){
        queue.addLast(e);
    }

    public ArrayList<Integer> pollFirst(){
        return queue.pollFirst();
    }

    @Override
    public int size() {
        return size;
    }

    public int queueSize() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @NonNull
    @Override
    public Iterator<ArrayList<Integer>> iterator() {
        return queue.iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] ts) {
        return queue.toArray(ts);
    }

    @Override
    public boolean add(ArrayList<Integer> e) {
        if(queue.size()<size) return queue.add(e);
        else return false;
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return queue.containsAll(collection);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends ArrayList<Integer>> collection) {
        if (queue.size()+collection.size()<=size) return queue.addAll(collection);
        else return false;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        return queue.removeAll(collection);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        return queue.retainAll(collection);
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public boolean offer(ArrayList<Integer> e) {
        return queue.offer(e);
    }

    @Override
    public ArrayList<Integer> remove() {
        return queue.remove();
    }

    @Override
    public ArrayList<Integer> poll() {
        return queue.poll();
    }

    @Override
    public ArrayList<Integer> element() {
        return queue.element();
    }

    @Override
    public ArrayList<Integer> peek() {
        return queue.peek();
    }

    public ArrayList<Integer> get(int index){
        return queue.get(index);
    }
}