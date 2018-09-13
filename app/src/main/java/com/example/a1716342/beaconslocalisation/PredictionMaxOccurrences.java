package com.example.a1716342.beaconslocalisation;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class PredictionMaxOccurrences implements Queue<Integer> {

    /*
    This class gives the prediction which is the most frequent in the list
    */

    //List of the ten last prediction (index of the classes)
    private LinkedList<Integer> queue;

    //Index of the last prediction
    private Integer lastPrediction;

    //List of the number of occurrences for each class
    private int[] listOccur;

    //Number of classes
    private int numberClass;

    //desired size for the
    private int size;

    public PredictionMaxOccurrences(int numberClass, int size){
        this.queue = new LinkedList<Integer>();
        this.numberClass = numberClass;
        this.lastPrediction = 0;
        this.listOccur = new int[numberClass];
        this.size = size;
    }

    public Integer max(){
        /*
        This method give the index of the prediction which has the greatest number of occurrences in the rssiAverage.
        If there is a equality, so one of the prediction having the greatest number of occurrences is the last prediction, so this one is given.
        */

        //initialise the list containing the number of occurrences
        for (int i = 0; i<numberClass; i++){
            listOccur[i] = 0;
        }

        //Fill the list containing the number of occurrences
        for (Integer p:queue){
            listOccur[p-1]+=1;
        }

        //Find greatest number of occurrences
        //Maximum number of occurrences
        int max = 0;
        //Index of the prediction which has the greatest number of occurrences
        int predMax = 0;
        for (int i = 0;i<listOccur.length; i++){
            if (listOccur[i] > max) {
                max = listOccur[i];
                predMax = i;
            }
        }

        //If the last prediction has the same number of occurrence that the prediction which has the max, so we still display the last prediction. In this case, we doesn't change anything.
        //Else, we display the new prediction.
        if (listOccur[predMax] != listOccur[lastPrediction]) lastPrediction = predMax;

        return predMax;
    }

    public String toString(){
        String s = "";
        for (Integer i:queue){
            s+=String.valueOf(i)+" ";
        }
        return s;
    }

    public void addLast(Integer e){
        queue.addLast(e);
    }

    public Integer pollFirst(){
        return queue.pollFirst();
    }

    public int getNumberClass() {
        return numberClass;
    }

    public void setNumberClass(int numberClass) {
        this.numberClass = numberClass;
    }

    public Integer getLastPrediction() {
        return lastPrediction;
    }

    public void setLastPrediction(Integer lastPrediction) {
        this.lastPrediction = lastPrediction;
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
    public Iterator<Integer> iterator() {
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
    public boolean add(Integer e) {
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
    public boolean addAll(@NonNull Collection<? extends Integer> collection) {
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
    public boolean offer(Integer e) {
        return queue.offer(e);
    }

    @Override
    public Integer remove() {
        return queue.remove();
    }

    @Override
    public Integer poll() {
        return queue.poll();
    }

    @Override
    public Integer element() {
        return queue.element();
    }

    @Override
    public Integer peek() {
        return queue.peek();
    }
}
