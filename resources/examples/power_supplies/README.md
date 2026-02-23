# Power Supply Examples

Complete power supply designs for various applications.

## Examples

| Example | Description | Difficulty | Status |
|---------|-------------|------------|--------|
| [Isolated DC-DC](isolated_dcdc/) | Flyback, Forward, Half-bridge | Intermediate | Placeholder |
| [PFC Converters](pfc_converters/) | Single and bridgeless PFC | Intermediate | Placeholder |
| [LLC Resonant](llc_resonant/) | High-efficiency resonant | Advanced | Placeholder |
| [DAB Converter](dab_converter/) | Dual Active Bridge, bidirectional | Advanced | Placeholder |

## Quick Reference

### Topology Selection by Power

| Power Range | Topology | Typical η |
|-------------|----------|-----------|
| 1-30W | Flyback | 85-90% |
| 30-150W | Flyback/Forward | 88-92% |
| 100-500W | Forward/Half-bridge | 90-94% |
| 300W-3kW | Full-bridge/LLC | 92-96% |
| 500W-5kW | LLC Resonant | 94-97% |
| 1-50kW | DAB (bidirectional) | 95-98% |

### Topology Comparison

| Feature | Flyback | Forward | LLC | DAB |
|---------|---------|---------|-----|-----|
| Isolation | Yes | Yes | Yes | Yes |
| Bidirectional | No | No | No | Yes |
| Soft switching | No | No | ZVS | ZVS |
| Complexity | Low | Medium | High | High |
| Best for | Low power | Medium | High η | Energy storage |

### Key Applications

| Application | Power | Topology |
|-------------|-------|----------|
| Phone charger | 5-20W | Flyback |
| Laptop adapter | 65-100W | Flyback/LLC |
| Server PSU | 500-2000W | LLC |
| EV OBC | 3-22kW | PFC + LLC/DAB |
| Energy storage | 10-100kW | DAB |
| DC microgrid | 5-50kW | DAB |

## Design Considerations

| Aspect | Flyback | Forward | LLC | DAB |
|--------|---------|---------|-----|-----|
| Component count | Low | Medium | High | High |
| Transformer | Gap (stores energy) | No gap | Resonant tank | HF isolation |
| Efficiency | Medium | Good | Excellent | Excellent |
| EMI | Higher | Medium | Lower | Lower |
| Control | Simple | Simple | Complex | Medium |

## Efficiency Breakdown

### LLC Resonant (1kW example)
| Loss | Value | % |
|------|-------|---|
| Primary switching | 5W | 0.5% |
| Transformer | 10W | 1.0% |
| Secondary rectification | 15W | 1.5% |
| Control | 5W | 0.5% |
| **Total** | **35W** | **3.5%** |
| **Efficiency** | | **96.5%** |

## Related Tutorials

- [Flyback Converter](../basic_topologies/flyback_converter/)
- [Forward Converter](../basic_topologies/forward_converter/)
- [302 - PFC Basics](../../tutorials/3xx_acdc_rectifiers/302_pfc_basics/)
