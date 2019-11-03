package com.parisoft.pa65.heap;

import com.parisoft.pa65.pojo.Alloc;
import com.parisoft.pa65.pojo.Block;
import com.parisoft.pa65.pojo.Function;
import com.parisoft.pa65.pojo.Ref;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.parisoft.pa65.util.VariableUtils.functionOf;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class Heap {

    private final Map<String, List<Block>> tmpHeapByFunction = new HashMap<>();
    private final Map<String, List<Block>> execHeapBySegment = new HashMap<>();
    private final Map<String, List<Block>> finalHeapBySegment = new LinkedHashMap<>();
    private final Map<String, List<Ref>> refsByFunction = new LinkedHashMap<>();
    private final Map<String, List<Ref>> refsByTarget = new LinkedHashMap<>();
    private final ArrayDeque<Function> stack = new ArrayDeque<>();

    public Map<String, List<Block>> getBlocksBySegment() {
        return finalHeapBySegment;
    }

    public Map<String, List<Block>> getBlocksByFunction() {
        return getBlocksBySegment().values()
                .stream()
                .flatMap(List::stream)
                .collect(groupingBy(Block::getFunction));
    }

    public Map<String, List<Ref>> getRefsByFunction() {
        return refsByFunction;
    }

    public ArrayDeque<Function> getStack() {
        return stack;
    }

    public void addReference(Function function, Ref ref) {
        List<Ref> refs = refsByFunction.computeIfAbsent(function.getName(), s -> new ArrayList<>());

        if (!refs.contains(ref)) {
            refs.add(ref);
        }

        refs = refsByTarget.computeIfAbsent(ref.getTargetVar(), s -> new ArrayList<>());

        if(!refs.contains(ref)) {
            refs.add(ref);
        }
    }

    public void save(Function function) {
        List<Block> blocks = execHeapBySegment.values()
                .stream()
                .flatMap(List::stream)
                .filter(this::canDereference)
                .collect(toList());
        List<Block> tmpHeap = tmpHeapByFunction.computeIfAbsent(function.getName(), s -> newHeap(false));
        blocks.forEach(block -> free(block, tmpHeap));
    }

    public void load(Function function) {
        List<Block> blocks = tmpHeapByFunction.remove(function.getName());
        blocks.stream()
                .map(Alloc::new)
                .forEach(this::allocByFirstFit);
    }

    public void free() {
        List<Block> blocks = execHeapBySegment.values()
                .stream()
                .flatMap(List::stream)
                .filter(this::canDereference)
                .collect(toList());
        blocks.forEach(this::free);
    }

    public void free(List<String> variables) {
        for (String variable : variables) {
            execHeapBySegment.values()
                    .stream()
                    .flatMap(List::stream)
                    .filter(block -> variable.equals(block.getVariable()))
                    .filter(this::canDereference)
                    .findFirst()
                    .ifPresent(this::free);
        }
    }

    private void free(Block block) {
        free(block, finalHeapBySegment.computeIfAbsent(block.getSegment(), s -> newHeap(false)));
    }

    private void free(Block block, List<Block> toHeap) {
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
            heap.add(i, free);
        }

        if (toHeap.stream().anyMatch(block1 -> Objects.equals(block.getVariable(), block1.getVariable()))) {
            return;
        }

        block.setFinished(true);
        toHeap.add(block);
    }

    @SuppressWarnings("SuspiciousListRemoveInLoop")
    public void allocByFirstFit(Alloc alloc) {
        List<Block> heap = execHeapBySegment.computeIfAbsent(alloc.getSegment(), s -> newHeap(true));

        if (heap.stream().anyMatch(block -> Objects.equals(alloc.getVariable(), block.getVariable()))) {
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
                .filter(Block::isNotFree)
                .filter(Block::isNotFinished)
                .collect(toList());
        heap.removeIf(Block::isNotFinished);
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

    private boolean canDereference(Block block) {
        if (block.isFree()) {
            return false;
        }

        return canDereference(block.getVariable());
    }

    private boolean canDereference(String variable) {
        String functionOfVariable = functionOf(variable);

        if (stack.stream().anyMatch(function -> function.getName().equals(functionOfVariable))) {
            return false;
        }

        List<String> pointers = refsByTarget.getOrDefault(variable, emptyList()).stream().map(Ref::getSourceVar).collect(toList());

        return pointers.stream().allMatch(this::canDereference);
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

    public static String nameOf(String segment) {
        return "heap_" + segment.replaceAll("\\W", "");
    }

}
