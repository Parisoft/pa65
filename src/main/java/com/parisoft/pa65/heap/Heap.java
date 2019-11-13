package com.parisoft.pa65.heap;

import com.parisoft.pa65.pojo.Alloc;
import com.parisoft.pa65.pojo.Block;
import com.parisoft.pa65.pojo.Function;
import com.parisoft.pa65.pojo.Ref;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.parisoft.pa65.util.VariableUtils.functionOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class Heap {

    private final Map<String, List<Block>> tmpHeapByFunction = new HashMap<>(); // temporary heap to save the state of the execution heap when jumping to another function
    private final Map<String, List<Block>> execHeapBySegment = new HashMap<>(); // execution heap: call to palloc or pfree creates or removes a block during processing
    private final Map<String, List<Block>> finalHeapBySegment = new LinkedHashMap<>(); // blocks for processed functions goes here
    private final Map<String, List<Ref>> refsByFunction = new LinkedHashMap<>();
    private final Map<String, List<Ref>> refsByTarget = new LinkedHashMap<>();
    private final Map<String, Set<String>> refCollisions = new HashMap<>();
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

    public void clear() {
        tmpHeapByFunction.clear();
        execHeapBySegment.clear();
        finalHeapBySegment.clear();
        refsByFunction.clear();
        refsByTarget.clear();
        stack.clear();
    }

    public void addReference(Function function, Ref ref) {
        List<Ref> refs = refsByFunction.computeIfAbsent(function.getName(), s -> new ArrayList<>());

        if (!refs.contains(ref)) {
            refs.add(ref);
        }

        refs = refsByTarget.computeIfAbsent(ref.getTargetVar(), s -> new ArrayList<>());

        if (!refs.contains(ref)) {
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

        for (Block block : blocks) {
            free(block);
            tmpHeap.add(block);
        }
    }

    public void load(Function function) throws AllocCollisionException {
        for (Block block : tmpHeapByFunction.remove(function.getName())) {
            allocByFirstFit(block.getAlloc());
        }
    }

    public void free(Function function) {
        free();

        execHeapBySegment.values()
                .stream()
                .flatMap(List::stream)
                .filter(block -> function.getName().equals(block.getFunction()))
                .forEach(block -> block.setFinished(true));
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

        List<Block> toHeap = finalHeapBySegment.computeIfAbsent(block.getSegment(), s -> newHeap(false));

        if (toHeap.stream().noneMatch(block1 -> Objects.equals(block.getVariable(), block1.getVariable()))) {
            block.setFinished(true);
            toHeap.add(block);
        }
    }

    public void allocByFirstFit(Alloc alloc) throws AllocCollisionException {
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
            allocFinalBlock(finalBlock, heap);
        } else {
            allocNewBlock(new Block(alloc), heap, 0);
        }
    }

    private void allocFinalBlock(Block block, List<Block> heap) throws AllocCollisionException {
        heap.removeIf(Block::isFree);

        List<Block> toReallocate = heap.stream()
                .filter(Block::isNotFinished)
                .filter(allocated -> allocated.overlaps(block))
                .collect(toList());

        heap.removeAll(toReallocate);
        heap.add(block);
        heap.sort(comparing(Block::getOffset).thenComparing(Block::isFinished, reverseOrder()));

        Optional<Block> collision = heap.stream()
                .filter(Block::isFinished)
                .filter(finished -> !finished.equals(block))
                .filter(finished -> finished.overlaps(block))
                .findFirst();

        if (collision.isPresent()) {
            Block other = collision.get();
            refCollisions.computeIfAbsent(block.getVariable(), s -> new HashSet<>()).add(other.getVariable());
            refCollisions.computeIfAbsent(other.getVariable(), s -> new HashSet<>()).add(block.getVariable());
            throw new AllocCollisionException();
        }

        fillFreeSpaces(heap);
        int validIndex = heap.indexOf(block) + 1;
        toReallocate.forEach(overlapped -> allocNewBlock(overlapped, heap, validIndex));
    }

    private void allocNewBlock(Block block, List<Block> heap, int fromIndex) {
        Optional<Block> overlapping = refCollisions.getOrDefault(block.getVariable(), emptySet())
                .stream()
                .map(variable -> Stream.concat(heap.stream(), finalHeapBySegment.getOrDefault(block.getSegment(), emptyList()).stream())
                        .filter(other -> Objects.equals(other.getVariable(), variable))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .max(comparingInt(Block::getOffsetPlusSize));

        for (int i = fromIndex; i < heap.size(); i++) {
            Block allocated = heap.get(i);

            if (allocated.isFree()) {
                if (overlapping.isPresent()) {
                    Block overlapped = overlapping.get();

                    if (allocated.getOffsetPlusSize() >= overlapped.getOffsetPlusSize() + block.getSize()) {
                        block.setOffset(overlapped.getOffsetPlusSize());

                        if (allocated.getOffset() < block.getOffset()) {
                            Block free = new Block();
                            free.setOffset(block.getOffsetPlusSize());
                            free.setSize(allocated.getOffsetPlusSize() - block.getOffset() - block.getSize());
                            allocated.setSize(allocated.getSize() - block.getSize() - free.getSize());
                            heap.add(i + 1, block);
                            heap.add(i + 2, free);
                        } else if (allocated.getOffset() > block.getOffset()) {
                            block.setOffset(allocated.getOffset());
                            allocated.setOffset(block.getOffsetPlusSize());
                            allocated.subSize(block);
                            heap.add(i, block);
                        } else {
                            allocated.setOffset(block.getOffsetPlusSize());
                            allocated.subSize(block);
                            heap.add(i, block);
                        }
                    } else {
                        continue;
                    }

                    return;
                } else {
                    if (allocated.getSize() > block.getSize()) {
                        allocated.subSize(block);
                        heap.add(i, block);
                    } else if (allocated.getSize() == block.getSize()) {
                        heap.set(i, block);
                    } else {
                        continue;
                    }

                    block.setOffset(allocated.getOffset());
                    allocated.setOffset(block.getOffsetPlusSize());

                    return;
                }
            }
        }
    }

    private static void fillFreeSpaces(List<Block> heap) {
        if (heap.isEmpty()) {
            Block free = new Block();
            free.setSize(Integer.MAX_VALUE);
            heap.add(free);
            return;
        }

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

            if (curr.getOffset() > prev.getOffsetPlusSize()) {
                if (prev.isFree()) {
                    prev.setSize(curr.getOffset() - prev.getOffset());
                } else {
                    Block free = new Block();
                    free.setOffset(prev.getOffsetPlusSize());
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
            free.setOffset(lastBlock.getOffsetPlusSize());
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

    public static class AllocCollisionException extends Exception {

    }
}
