package com.rose.editor.common;


import com.rose.editor.model.BlockLine;
import com.rose.editor.model.NavigationLabel;
import com.rose.editor.model.Span;

import java.util.List;

/**
 * A object provider for speed improvement
 * Now meaningless because it is not as well as it expected
 * @author Rose
 */
public class Allocator {

    private List<Span> cache;

    private int max = 1024 * 128;

    public void addSource(List<Span> src) {
        if(src == null) {
            return;
        }
        if(cache == null) {
            cache = src;
            return;
        }
        int size = cache.size();
        int sizeAnother = src.size();
        while(sizeAnother > 0 && size < max) {
            size++;
            sizeAnother--;
            cache.add(src.get(sizeAnother));
        }
    }

    public Span next() {
        int size_t;
        if(cache == null || (size_t = cache.size()) == 0) {
            return new Span(0,0);
        }
        return cache.remove(size_t - 1);
    }

    public Span obtain(int p1,int p2) {
        Span span = next();
        span.startIndex = p1;
        span.colorId = p2;
        return span;
    }

    public Span obtain(int p1,int p2,int p3,int p4) {
        Span span = next();
        span.startIndex = p1;
        span.line = p2;
        span.column = p3;
        span.colorId = p4;
        return span;
    }

    private List<BlockLine> cache2;
    private int max2 = 1024 * 8;

    public void addSource2(List<BlockLine> src) {
        if(src == null) {
            return;
        }
        if(cache2 == null) {
            cache2 = src;
            return;
        }
        int size = cache2.size();
        int sizeAnother = src.size();
        while(sizeAnother > 0 && size < max2) {
            size++;
            sizeAnother--;
            cache2.add(src.get(sizeAnother));
        }
    }

    public BlockLine next2(){
        return (cache2 == null || cache2.isEmpty()) ? new BlockLine() : cache2.remove(cache2.size() - 1);
    }


    private List<NavigationLabel> cache3;
    private int max3 = 1024 * 8;

    public void addSource3(List<NavigationLabel> src) {
        if(src == null) {
            return;
        }
        if(cache3 == null) {
            cache3 = src;
            return;
        }
        int size = cache3.size();
        int sizeAnother = src.size();
        while(sizeAnother > 0 && size < max3) {
            size++;
            sizeAnother--;
            cache3.add(src.get(sizeAnother));
        }
    }

    public NavigationLabel next3(int line,String label){
        return (cache3 == null || cache3.isEmpty()) ? new NavigationLabel(line,label) : cache3.remove(cache3.size() - 1);
    }
}
