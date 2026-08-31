package me.drex.polymerpatcher.config;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockConfig {
    public int defaultWeight = 1000;

    public Map<Identifier, Integer> weights = new HashMap<>() {{

    }};

    public List<RegexWeight> regexWeights = new ArrayList<>(){{
        add(new RegexWeight(".*_leaves", 250));
        add(new RegexWeight(".*_log", 500));
        add(new RegexWeight(".*_wood", 750));
    }};

    public record RegexWeight(String regex, int weight) {}

}
