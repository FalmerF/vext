package ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.vext.engine.vulkan.buffer.ImageView;
import ru.vext.engine.vulkan.buffer.MemoryBuffer;
import ru.vext.engine.vulkan.buffer.UniformBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class DescriptorSetLayout {

    private final AbstractDescriptorBinding[] bindings;

    public static Builder builder() {
        return new Builder();
    }

    public Map<Integer, Integer> getDescriptorsCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (AbstractDescriptorBinding binding : bindings) {
            int count = counts.getOrDefault(binding.getType(), 0);
            counts.put(binding.getType(), count + 1);
        }
        return counts;
    }

    @Setter
    public static class Builder {

        private List<AbstractDescriptorBinding> bindings = new ArrayList<>();

        public Builder addBinding(AbstractDescriptorBinding binding) {
            binding.setBinding(bindings.size());
            bindings.add(binding);
            return this;
        }

        public Builder addGpuBufferBinding(MemoryBuffer buffer, int flags) {
            bindings.add(new GpuBufferBinding(buffer, flags, bindings.size()));
            return this;
        }

        public Builder addImageBinding(ImageView imageView, int flags) {
            bindings.add(new ImageBinding(imageView, flags, bindings.size()));
            return this;
        }

        public Builder addUniformBufferBinding(UniformBuffer buffer, int flags) {
            bindings.add(new UniformBinding(buffer, flags, bindings.size()));
            return this;
        }

        public DescriptorSetLayout build() {
            return new DescriptorSetLayout(bindings.toArray(AbstractDescriptorBinding[]::new));
        }
    }
}
