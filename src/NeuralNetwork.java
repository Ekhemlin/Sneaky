import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author hydrozoa
 */
public abstract class NeuralNetwork {

    public Map<Integer, Neuron> neurons; // All neurons in genome, mapped by ID

    public List<Integer> input; 	// IDs of input neurons
    public List<Integer> output;	// IDs of output neurons

    private List<Neuron> unprocessed;
    Genome genome;

    //this should only be called when theres a new genome
    public NeuralNetwork(Genome genome) {
        input = new ArrayList<Integer>();
        output = new ArrayList<Integer>();
        neurons = new HashMap<Integer, Neuron>();
        unprocessed = new LinkedList<Neuron>();
        this.genome = genome;

        //process all in nodes
        for (Integer nodeID : genome.getNodeGenes().keySet()) {
            NodeGene node = genome.getNodeGenes().get(nodeID);
            Neuron neuron = new Neuron();

            if (node.getType() == NodeGene.TYPE.INPUT) {
                neuron.addInputConnection();
                input.add(nodeID);
            } else if (node.getType() == NodeGene.TYPE.OUTPUT) {
                output.add(nodeID);
            }
            neurons.put(nodeID, neuron);
        }

        //process all the connections
        for (Integer connID : genome.getConnectionGenes().keySet()) {
            ConnectionGene conn = genome.getConnectionGenes().get(connID);
            if (!conn.isExpressed()) {
                continue;
            }
            Neuron inputter = neurons.get(conn.getInNode()); // the node that leads into the connection, is the inputter to the connection
            //System.out.println("Adding output from nodeID="+conn.getInNode()+"\t to\t nodeID="+conn.getOutNode()+"\tweight="+conn.getWeight());
            inputter.addOutputConnection(conn.getOutNode(), conn.getWeight());
            Neuron outputReceiver = neurons.get(conn.getOutNode()); // the node that receives from the connection, is the output receiver of the connection
            //System.out.println("Adding input to nodeID="+conn.getOutNode());
            outputReceiver.addInputConnection();
        }
    }

    public float [] runCalculation() {

        int [] params = getInput();
        float[] floats = new float[4];
        for(int i=0; i<4; i++){
            floats[i] = (float)params[i];
        }
        if(floats.length!=4){
            System.out.print(floats.length);
        }
        return calculate(floats);
    }

    public abstract int[] getInput();


    public float[] calculate(float[] input_parameter) {

        //System.out.println("New round of calculation...");
        if (input_parameter.length != input.size()) {
            GenomePrinter.printGenome(this.genome, "/Users/eitankhemlin/Desktop/code/crash.png");
            throw new IllegalArgumentException("Number of inputs must match number of input neurons in genome");

        }

        for (Integer key : neurons.keySet()) {
            Neuron neuron = neurons.get(key);
            neuron.reset();
        }

        unprocessed.clear();
        unprocessed.addAll(neurons.values());

        // ready the inputs
        for (int i = 0; i < input_parameter.length; i++) { 								// loop through each input
            Neuron inputNeuron = neurons.get(input.get(i));
           // System.out.println("Feeding input ("+input_parameter[i]+") into input neuron (id="+input.get(i)+")\n");
            inputNeuron.feedInput(input_parameter[i]);									// input neurons only have one input, so we know they're ready for calculation
            inputNeuron.calculate();
            //System.out.println("Calculating node="+input.get(i)+"   result="+inputNeuron.output+"\n");

            for (int k = 0; k < inputNeuron.outputIDs.size(); k++) { 			// loop through receivers of this input
                Neuron receiver = neurons.get(inputNeuron.outputIDs.get(k));
            //    System.out.println("Feeding input ("+inputNeuron.getOutput()* inputNeuron.outputWeights.get(k) +") into inputneuron (id="+inputNeuron.outputIDs.get(k)+")\n");
                receiver.feedInput(inputNeuron.getOutput()*inputNeuron.outputWeights.get(k));		// add the input directly to the next neuron, using the correct weight for the connection
            }
            unprocessed.remove(inputNeuron);
        }

        int loops = 0;
        while (unprocessed.size() > 0) {
            loops++;
            if (loops > 1000) {
                //System.out.println("Can't solve network... Giving up to return null");
                return null;
            }

            Iterator<Neuron> it = unprocessed.iterator();
            while (it.hasNext()) {
                Neuron n = it.next();
                if (n.isReady()) {						// if neuron has all inputs, calculate the neuron
                    n.calculate();
                    //System.out.println("Output value = "+n.getOutput());
                    for (int i = 0; i < n.outputIDs.size(); i++) {
                        int receiverID = n.outputIDs.get(i);
                        float receiverValue = n.output * n.outputWeights.get(i);
                        //System.out.println("Feeding input ("+receiverValue+") into inputneuron (id="+receiverID+")");
                        neurons.get(receiverID).feedInput(receiverValue);
                    }
                    it.remove();
                }
            }
        }
        //System.out.println("Solved the network after "+loops+" loops");

        // copy output from output neurons, and copy it into array
        float[] outputs = new float[output.size()];
        for (int i = 0; i < output.size(); i++) {
            outputs[i] = neurons.get(output.get(i)).getOutput();
        }

        return outputs;
    }

    public static class Neuron {

        //value outputted after calculation
        private float output;
        //value of inputs
        private List<Float> inputs;

        //neurons to output to
        private List<Integer> outputIDs;
        //weights of connecting output connections
        private List<Float> outputWeights;

        public Neuron() {
            inputs = new ArrayList<>();
            outputIDs = new ArrayList<>();
            outputWeights = new ArrayList<>();
        }

        /**
         * Adds a connection from this neuron to another neuron
         * @param outputID 	ID of the target neuron
         * @param weight	Weight on this connection
         */
        public void addOutputConnection(int outputID, float weight) {
            outputIDs.add(outputID);

            outputWeights.add(weight);
        }

        /**
         * Adds a connection to this neuron from another neuron
         */
        public void addInputConnection() {
            //System.out.println("Adding input connection, current length is "+inputs.length);
            inputs.add(0.0f);
            for (int i = 0; i < inputs.size();i++) {
                inputs.set(i,null);
            }
            //System.out.println("Finished adding input connection, current length is "+inputs.length);
        }

        /**
         * Takes all the inputs, and calculates them into an output
         * This can only happen if the neuron {@link #isReady() isReady}
         */
        public float calculate() {
            float sum = 0f;
            for (Float f : inputs) {
                sum += f;
            }
            output = sigmoidActivationFunction(sum);
            return output;
        }

        /**
         * If a neuron is ready, it has all the needed inputs to do calculation
         * @return true if ready
         */
        public boolean isReady() {
            boolean foundNull = false;
            for (Float f : inputs) {
                if (f == null) {
                    foundNull = true;
                    break;
                }
            }
            return !foundNull;
        }

        /**
         * Adds an input to the neuron in the first slot available
         */
        public void feedInput(float input) {
            //System.out.println("Feeding input\tInput slots total: "+inputs.length);
            boolean foundSlot = false;
            for (int i = 0; i < inputs.size(); i++) {
                if (inputs.get(i) == null) {
                    inputs.set(i,input);
                    foundSlot = true;
                    break;
                }
            }
            if (!foundSlot) {
                throw new RuntimeException("No input slot ready for input. Input array: "+inputs.toString());
            }
        }

        /**
         * @return	Output of this neuron
         */
        public float getOutput() {
            return output;
        }

        /**
         * Resets the inputs on this neuron, as well as the calculation
         */
        public void reset() {
            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i,null);
            }
            output = 0f;
        }

        /* Takes any float, and returns a value between 0 and 1. 0f returns 0.5f */
        public static float sigmoidActivationFunction(float in) {
            return (float)(1f/( 1f + Math.exp(-4.9d*in)));
        }
    }

}