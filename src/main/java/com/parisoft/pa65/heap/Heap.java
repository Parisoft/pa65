package com.parisoft.pa65.heap;

import com.parisoft.pa65.pojo.Alloc;
import com.parisoft.pa65.pojo.Block;
import com.parisoft.pa65.pojo.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class Heap {

    private final Map<String, List<Block>> execHeapBySegment = new HashMap<>();
    private final Map<String, List<Block>> finalHeapBySegment = new HashMap<>();

    public Map<String, List<Block>> getBlocksBySegment() {
        return finalHeapBySegment;
    }

    public Map<String, List<Block>> getBlocksByFunction(){
        return getBlocksBySegment().values()
                .stream()
                .flatMap(List::stream)
                .collect(groupingBy(Block::getFunction));
    }

    public void free(Function function){
        List<Block> blocks = execHeapBySegment.values()
                .stream()
                .flatMap(List::stream)
                .filter(block -> block.belongsTo(function))
                .collect(toList());
        blocks.forEach(this::free);
    }

    public void free(List<String> variables) {
        for (String variable : variables) {
            execHeapBySegment.values()
                    .stream()
                    .flatMap(List::stream)
                    .filter(block -> variable.equals(block.getVariable()))
                    .findFirst()
                    .ifPresent(this::free);
        }
    }

    public void free(Block block) {
        List<Block> heap = execHeapBySegment.get(block.getSegment());
        int i = IntStream.range(0, heap.size()).filter(value -> heap.get(value).equals(block)).findFirst().orElse(-1);

        if (i < 0) {
            return;
        }

        Block prev = i > 0 ? heap.get(i - 1) : null;
        Block next = i < heap.size() - 1 ? heap.get(i + 1) : null;
        Block curr = heap.remove(i);

        if (prev != null && prev.isFree()) {
            prev.addSize(curr);

            if (next != null && next.isFree()) {
                prev.addSize(next);
                heap.remove(next);
            }
        } else if (next != null && next.isFree()) {
            next.addSize(curr);
            next.setOffset(curr.getOffset());
        } else {
            Block free = new Block();
            free.setSize(block.getSize());
            free.setOffset(block.getOffset());
            heap.add(i, block);
        }

        block.setFinished(true);
        finalHeapBySegment.computeIfAbsent(block.getSegment(), s -> newHeap(false)).add(block);
    }

    public void allocByFirstFit(Alloc alloc) {
        List<Block> heap = execHeapBySegment.computeIfAbsent(alloc.getSegment(), s -> newHeap(true));

        if (heap.stream().anyMatch(block -> alloc.getVariable().equals(block.getVariable()))) {
            return;
        }

        Block finalBlock = finalHeapBySegment.computeIfAbsent(alloc.getSegment(), s -> newHeap(false))
                .stream()
                .filter(block -> alloc.getVariable().equals(block.getVariable()))
                .findFirst()
                .orElse(null);

        if (finalBlock != null) {
            heap.add(finalBlock);
            realloc(heap);
            return;
        }

        Block newBlock = new Block(alloc);

        if (heap.isEmpty()) {
            heap.add(newBlock);
            return;
        }

        for (int i = 0; i < heap.size(); i++) {
            Block block = heap.get(i);

            if (block.isFree()) {
                if (block.getSize() > newBlock.getSize()) {
                    block.subSize(newBlock);
                } else if (block.getSize() == newBlock.getSize()) {
                    heap.remove(i);
                } else {
                    continue;
                }

                newBlock.setOffset(block.getOffset());
                block.setOffset(newBlock.getOffset() + newBlock.getSize());
                heap.add(i, newBlock);

                return;
            }
        }
    }

    private void realloc(List<Block> heap) {
        List<Block> newBlocks = heap.stream()
                .filter(block -> !block.isFree())
                .filter(block -> !block.isFinished())
                .collect(toList());
        heap.removeIf(block -> !block.isFinished());
        fillFreeSpaces(heap);
        newBlocks.stream().map(Alloc::new).forEach(this::allocByFirstFit);
    }

    private static void fillFreeSpaces(List<Block> heap) {
        if (heap.isEmpty()) {
            Block free = new Block();
            free.setSize(Integer.MAX_VALUE);
            heap.add(free);
            return;
        }

        heap.sort(comparing(Block::getOffset));

        for (int i = 0; i < heap.size(); i++) {
            Block prev;
            Block curr = heap.get(i);

            if (i == 0) {
                if (curr.getOffset() > 0) {
                    prev = new Block();
                    prev.setSize(curr.getOffset());
                    heap.add(0, prev);
                }

                continue;
            }

            prev = heap.get(i - 1);

            if (curr.getOffset() > prev.getOffset() + prev.getSize()) {
                if (prev.isFree()) {
                    prev.setSize(curr.getOffset() - prev.getOffset());
                } else {
                    Block free = new Block();
                    free.setOffset(prev.getOffset() + prev.getSize());
                    free.setSize(curr.getOffset() - free.getOffset());
                    heap.add(i, free);
                }
            }
        }

        Block lastBlock = heap.get(heap.size() - 1);

        if (lastBlock.isFree()) {
            lastBlock.setSize(Integer.MAX_VALUE);
        } else {
            Block free = new Block();
            free.setOffset(lastBlock.getOffset() + lastBlock.getSize());
            free.setSize(Integer.MAX_VALUE);
            heap.add(heap.size(), free);
        }
    }

    private static List<Block> newHeap(boolean fill) {
        List<Block> heap = new ArrayList<>();

        if (fill) {
            Block free = new Block();
            free.setSize(Integer.MAX_VALUE);
            heap.add(free);
        }

        return heap;
    }
}
