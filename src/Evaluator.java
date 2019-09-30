import java.util.*;

/**
 * @author hydrozoa
 */
public abstract class Evaluator {

    private NEATConfiguration config;
    private FitnessGenomeComparator comparator = new FitnessGenomeComparator();

    protected List<Genome> genomes;						// stores all genomes of current generation
    protected List<Genome> nextGeneration;				// stores next generation of genomes (used during evaluation)
    protected List<FitnessGenome> evaluatedGenomes;		// stores all genomes with fitness of current generation (used during evaluation). Incidentally, this list contains results from previous generation.
    protected FitnessGenome fittestGenome;				// fittest genome w/ score form last run generation

    protected List<FitnessGenome> lastGenerationResults;	// contains a sorted list of the previous generations genomes

    protected InnovationGenerator nodeInnovation;
    protected InnovationGenerator connectionInnovation;


    public Evaluator(NEATConfiguration configuration, InitialGenomeProvider generator, InnovationGenerator nodeInnovation, InnovationGenerator connectionInnovation) {
        this.config = configuration;

        genomes = new ArrayList<Genome>(configuration.getPopulationSize());
        for (int i = 0; i < configuration.getPopulationSize(); i++) {
            Genome g = generator.generatFirstGenome();
            genomes.add(g);
        }

        evaluatedGenomes = new LinkedList<FitnessGenome>();
        nextGeneration = new LinkedList<Genome>();

        lastGenerationResults = new LinkedList<FitnessGenome>();

        this.nodeInnovation = nodeInnovation;
        this.connectionInnovation = connectionInnovation;
    }


    public void evaluateGeneration(Random r) {
        lastGenerationResults.clear();
        evaluatedGenomes.clear();

        /* Score each genome */
        for (int i = 0; i < genomes.size(); i++) {
            Genome g = genomes.get(i);
            FitnessGenome fitnessGenome = new FitnessGenome(g, evaluateGenome(g));
            evaluatedGenomes.add(fitnessGenome);
        }

        /* Sort evaluated genomes by fitness */
        Collections.sort(evaluatedGenomes, comparator);
        Collections.reverse(evaluatedGenomes);

        lastGenerationResults.addAll(evaluatedGenomes);

        fittestGenome = evaluatedGenomes.get(0);

        /* Kill off worst 9/10 of genomes */
        int cutoffIndex = evaluatedGenomes.size() / 10;
        Iterator<FitnessGenome> it = evaluatedGenomes.iterator();
        int index = 0;
        while (it.hasNext()) {
            it.next();
            if (index > cutoffIndex) {
                it.remove();
            }
            index++;
        }

        /* Find next generation population */
        nextGeneration.clear();

        // First, take champion of this generation and pass on to next generation
        Genome champion = evaluatedGenomes.get(0).genome;
        nextGeneration.add(champion);

        // Next, fill in next generation by random mating and mutation
        while (nextGeneration.size() < config.getPopulationSize()) {
            if (r.nextFloat() > config.ASEXUAL_REPRODUCTION_RATE) { // sexual reproduction
                FitnessGenome parent1 = evaluatedGenomes.get(r.nextInt(evaluatedGenomes.size()));
                FitnessGenome parent2 = evaluatedGenomes.get(r.nextInt(evaluatedGenomes.size()));
                Genome child;
                if (parent1.fitness > parent2.fitness) {
                    child = Genome.crossover(parent1.genome, parent2.genome, r,config.DISABLED_GENE_INHERITING_CHANCE);
                } else {
                    child = Genome.crossover(parent2.genome, parent1.genome, r,config.DISABLED_GENE_INHERITING_CHANCE);
                }
                if (r.nextFloat() < config.MUTATION_RATE) {
                    child.mutation(config.PERTURBING_RATE, r);
                }
                if (r.nextFloat() < config.ADD_CONNECTION_RATE) {	// add mutation from adding connection and nodes
                    child.addConnectionMutation(r, connectionInnovation, 100);
                }
                if (r.nextFloat() < config.ADD_NODE_RATE) {	// add mutation from adding node
                    child.addNodeMutation(r, connectionInnovation, nodeInnovation);
                }
                nextGeneration.add(child);
            } else {												// asexual reproduction
                FitnessGenome parent = evaluatedGenomes.get(r.nextInt(evaluatedGenomes.size()));
                Genome child = new Genome(parent.genome);
                child.mutation(config.PERTURBING_RATE, r);
                nextGeneration.add(child);
            }
        }

        // Transfer next generation to current generation
        genomes.clear();
        for (int i = 0; i < nextGeneration.size(); i++) {
            genomes.add(nextGeneration.get(i));
        }
    }

    public abstract float evaluateGenome(Genome g);

    /**
     * @return Fittest genome from the previous generation.
     */
    public FitnessGenome getFittestGenome() {
        return fittestGenome;
    }

    public int getGenomeAmount() {
        return genomes.size();
    }

    /**
     * @return	All genomes in the current generation. These are not evaluated yet!
     */
    public Iterable<Genome> getGenomes() {
        return genomes;
    }

    /**
     * @return	Results from previously evaluated generation, or null if no evaluation has taken place.
     */
    public Iterable<FitnessGenome> getLastGenerationResults() {
        return lastGenerationResults;
    }
    public class FitnessGenomeComparator implements Comparator<FitnessGenome> {

        /**
         * @return 	a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(FitnessGenome one, FitnessGenome two) {
            if (one.fitness > two.fitness) {
                return 1;
            } else if (one.fitness < two.fitness) {
                return -1;
            }
            return 0;
        }

    }
    public class FitnessGenome {

        public float fitness;
        public Genome genome;

        public FitnessGenome(Genome genome, float fitness) {
            this.genome = genome;
            this.fitness = fitness;
        }
    }
}