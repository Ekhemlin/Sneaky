public class ConnectionGene {
    private int inNode;
    private int outNode;
    private float weight;
    private boolean expressed;
    private int innovation;


    public ConnectionGene(int inNode, int outNode, float weight, boolean expressed, int innovation){
        this.inNode = inNode;
        this.outNode = outNode;
        this.weight = weight;
        this.expressed = expressed;
        this.innovation = innovation;
    }

    public ConnectionGene(ConnectionGene copied) {
        this.inNode = copied.inNode;
        this.outNode = copied.outNode;
        this.weight = copied.weight;
        this.expressed = copied.expressed;
        this.innovation = copied.innovation;
    }

    public ConnectionGene copy(){
        return new ConnectionGene(inNode, outNode, weight, expressed, innovation);
    }


    public int getInNode(){
        return this.inNode;
    }
    public int getOutNode(){
        return this.outNode;
    }
    public float getWeight(){
        return this.weight;
    }
    public boolean isExpressed(){
        return this.expressed;
    }
    public int getInnovation(){
        return this.innovation;
    }
    public void disable(){ expressed = false; }
    public void setWeight(float newWeight){
        this.weight = newWeight;
    }


}
