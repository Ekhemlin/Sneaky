
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.KeyListener;


public class Main extends JFrame implements ActionListener {


    final int B_WIDTH = 800;
    final int B_HEIGHT = 800;


    public class Eater {
        int xCord;
        int yCord;
        int points;
        public boolean crashed = false;
        int direction;
        int speed;

        public Eater() {
            xCord = B_WIDTH/2;
            yCord = B_HEIGHT/2;
            direction = 4;
            points = 0;
            speed = 10;
        }

        public void reset() {
            this.xCord = B_HEIGHT/2;;
            this.yCord = B_WIDTH/2;
            this.direction = 4;
        }

    }
    float dist(int x1, int x2, int y1, int y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public boolean debug = false;

    public final float ACTIVATION_THRESHOLD = 0.5f;


    int radius = 10;
    int cakeRadius = 60;
    int cakeBorderBuffer = cakeRadius;

    int populationSize = 200;
    public Eater [] eaters = new Eater[populationSize];
    Evaluator eval;
    NEATConfiguration conf = new NEATConfiguration(populationSize);
    public NeuralNetwork [] neuralNetworks = new NeuralNetwork[populationSize];
    Eater debugEater;
    private GameCanvas canvas;
    Timer timer;
    int cakeX = cakeBorderBuffer;
    int cakeY = cakeBorderBuffer;
    int genCount=0;
    Random r = new Random();
    InnovationGenerator nodeInnovationGenerator = new InnovationGenerator();
    InnovationGenerator conInnovationGenerator = new InnovationGenerator();

    int highestScore =0;




    public Genome generateFirstGenome(){
        InnovationGenerator nodeInnovation = nodeInnovationGenerator;
        InnovationGenerator conInnovation = conInnovationGenerator;
        Genome firstGenome = new Genome();

        int firstInputIn = 0;
        int firstOutputIn = 0;
        int[] inputs = new int[4];
        int[] outputs = new int[4];



        for (int i = 0; i < 4; i++) {
            int inputIn = nodeInnovation.getInnovation();
            NodeGene input = new NodeGene(NodeGene.TYPE.INPUT, inputIn);


            firstGenome.getNodeGenes().put(input.getId(),input);
            inputs[i] = inputIn;

            int outputIn = nodeInnovation.getInnovation();
            NodeGene output = new NodeGene(NodeGene.TYPE.OUTPUT, outputIn);
            firstGenome.getNodeGenes().put(output.getId(), output);
            outputs[i] = outputIn;
        }
        for(int i=0;i<2;i++){
            for(int j=0; j<4;j++){
                int conIn = conInnovation.getInnovation();
                ConnectionGene con = new ConnectionGene(inputs[i], outputs[j],(float)r.nextGaussian(), true, conIn);
                firstGenome.addConnectionGene(con);
            }
        }

        return firstGenome;
    }


    private void relocateCake() {
        int newCakeX = cakeBorderBuffer;
        int newCakeY = cakeBorderBuffer;
        if (debug) {
            float newCakeDistance = 0;
            do{
                newCakeX = cakeBorderBuffer + new Random().nextInt(B_WIDTH - 2*cakeBorderBuffer);
                newCakeY = cakeBorderBuffer + new Random().nextInt(B_HEIGHT - 2* cakeBorderBuffer);
                newCakeDistance = dist(newCakeX + cakeRadius, debugEater.xCord + radius, newCakeY + cakeRadius, debugEater.yCord + radius);
            } while (newCakeDistance <= cakeRadius);
        }
        else {

            newCakeX = cakeBorderBuffer + new Random().nextInt(B_WIDTH - 2*cakeBorderBuffer);
            newCakeY = cakeBorderBuffer + new Random().nextInt(B_HEIGHT - 2* cakeBorderBuffer);

            //EITAN test this
            // Try to relocate to a place that doesn't have an eater. Give up if it doesnt work


//
//            float minDistanceFromEaters = 0; // need to MAX this
//
//            for (int i = 0; i < populationSize*2; i++) {
//                int testCakeX = cakeBorderBuffer + new Random().nextInt(B_WIDTH - cakeBorderBuffer);
//                int testCakeY = cakeBorderBuffer + new Random().nextInt(B_HEIGHT - cakeBorderBuffer);
//                float minDistance = 1000;
//
//                for(int j=0; j<populationSize; j++) {
//                    Eater testEater = eaters[j];
//                    float testDist = dist(newCakeX + cakeRadius, testEater.xCord + radius, newCakeY + cakeRadius, testEater.yCord + radius);
//                    if (testDist < cakeRadius) {
//                        break;
//                    } else {
//                        if (testDist < minDistance) {
//                            minDistance = testDist;
//                        }
//                    }
//                }
//                if(minDistance>minDistanceFromEaters){
//                    newCakeX = testCakeX;
//                    newCakeY = testCakeY;
//                }
        }
        cakeX = newCakeX;
        cakeY = newCakeY;
    }

    public Main() {

            if (debug) {
                debugEater = new Eater();
            }
            else {

                InitialGenomeProvider provider = new InitialGenomeProvider() {
                    @Override
                    public Genome generatFirstGenome() {
                        return generateFirstGenome();
                    }
                };

                eval = new Evaluator(conf, provider, nodeInnovationGenerator, conInnovationGenerator) {
                    @Override
                    public float evaluateGenome(Genome g) {
                        return 0;
                    }
                };


                for (int i = 0; i < populationSize; i++) {
                    final int eaterIndex = i;
                    eaters[eaterIndex] = new Eater();
                    neuralNetworks[i] = new NeuralNetwork(eval.genomes.get(eaterIndex)) {
                        @Override
                        public int[] getInput() {
                            Eater eater = eaters[eaterIndex];
                            int[] params = new int[4];
                            params[0] = eater.xCord;
                            params[1] = eater.yCord;
                            params[2] = cakeX;
                            params[3] = cakeY;
                            return params;
                        }
                    };

                }

            }
            canvas = new GameCanvas();
            canvas.setPreferredSize(new Dimension(B_HEIGHT, B_WIDTH));
            Container cp = getContentPane();
            cp.add(canvas);
            setFocusTraversalKeysEnabled(true);


            this.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    if (keyCode == KeyEvent.VK_W) {
                        debugEater.direction = 0;
                    }
                    if (keyCode == KeyEvent.VK_D) {
                        debugEater.direction = 1;
                    }
                    if (keyCode == KeyEvent.VK_S) {
                        debugEater.direction = 2;
                    }
                    if (keyCode == KeyEvent.VK_A) {
                        debugEater.direction = 3;
                    }
                }

                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }

            });
            //EITAN maybe 0?
            timer = new Timer(20, this);
            timer.start();
            pack();
            setTitle("Sneaky");
            setVisible(true);

        }

        @Override
        public void actionPerformed (ActionEvent e){
            if (debug) {
                // Checking for loss
                if (debugEater.xCord < 0 || debugEater.xCord > B_WIDTH || debugEater.yCord < 0 || debugEater.yCord > B_HEIGHT) {
                    debugEater.crashed = true;
                    debugEater.reset();
                    relocateCake();
                    debugEater.points = 0;
                }
                // Adding points for living
                if (debugEater.direction >= 0 && debugEater.direction <= 3) {
                    debugEater.points += 1;
                }

                // Movements
                if (debugEater.direction == 0) {
                    debugEater.yCord -= debugEater.speed;
                } else if (debugEater.direction == 1) {
                    debugEater.xCord += debugEater.speed;
                } else if (debugEater.direction == 2) {
                    debugEater.yCord += debugEater.speed;
                } else if (debugEater.direction == 3) {
                    debugEater.xCord -= debugEater.speed;
                }

                if (dist(cakeX + cakeRadius, debugEater.xCord + radius, cakeY + cakeRadius, debugEater.yCord + radius) < cakeRadius) {
                    debugEater.points += 1000;
                    relocateCake();
                }
                canvas.repaint();
            }
            else {
                float [] results;
                boolean relocateCake = false;
                boolean allEatersLost = true;
                for(int i=0; i<populationSize; i++) {
                    Eater eater = eaters[i];
                    if(eater.crashed) continue;


                    results = neuralNetworks[i].runCalculation();
                    if(results.length==3){
                        GenomePrinter.printGenome(neuralNetworks[i].genome, "/Users/eitankhemlin/Desktop/code/3out.png");

                    }

                    eater.yCord-=Math.floor(eater.speed*results[0]);
                    eater.xCord += Math.floor(eater.speed*results[1]);
                    eater.yCord += Math.floor(eater.speed*results[2]);
                    eater.xCord -= Math.floor(eater.speed*results[3]);
                    if(eater.points%100==0){
                        eater.speed*=2;
                    }
                    if(eater.points>highestScore){
                        highestScore = eater.points;
                    }

//
//                    if(results[0]>ACTIVATION_THRESHOLD) {
//                        eater.yCord -= eater.speed;
//                        eater.direction = 0;
//                        System.out.print("0");
//                    }
//
//                   if(results[2]>ACTIVATION_THRESHOLD) {
//                        eater.yCord += eater.speed;
//                        eater.direction = 2;
//                       System.out.print("2");
//                    }
//
//                    if(results[1]>ACTIVATION_THRESHOLD) {
//                        eater.xCord += eater.speed;
//                        eater.direction = 1;
//                        System.out.print("1");
//                    }
//                    if(results[3]>ACTIVATION_THRESHOLD) {
//                        eater.xCord -= eater.speed;
//                        eater.direction = 3;
//                        System.out.print("3");
//                    }
                    eater.points+=eater.speed/10;
                    if (dist(cakeX + cakeRadius, eater.xCord + radius, cakeY + cakeRadius, eater.yCord + radius) < cakeRadius) {
                        eater.points += 100*eater.speed;
                        relocateCake = true;
                    }
                    if (eater.xCord <= 0 || eater.xCord > B_WIDTH || eater.yCord < 0 || eater.yCord > B_HEIGHT) {
                        eater.reset();
                        eater.crashed = true;
                        eater.points*=0.3;
                    }
                    if(eater.xCord == B_WIDTH/2 && eater.yCord==B_HEIGHT/2&&eater.points>100){
                        eater.crashed = true;
                        eater.points*=0.3;
                    }

                    if(!eater.crashed) {
                        allEatersLost = false;
                    }
                    if(eater.points%50==0){
                        eater.speed*=2;
                    }

                }
                if(relocateCake) relocateCake();
                canvas.repaint();

                if(allEatersLost) {
                    genCount++;

                    System.out.print("ALL LOST");
                    System.out.print("printing");
                    eval.evaluateGeneration(r);
                    System.out.print(eval.getFittestGenome().genome.getNodeGenes().size());
                    if(genCount%20==0) {
                        GenomePrinter.printGenome(eval.getFittestGenome().genome, "/Users/eitankhemlin/Desktop/code/fittest"+Integer.toString(genCount)+".png");
                    }
                    for (int i = 0; i < populationSize; i++) {
                        final int eaterIndex = i;
                        eaters[eaterIndex] = new Eater();
                        NeuralNetwork nn = new NeuralNetwork(eval.genomes.get(eaterIndex)) {
                            @Override
                            public int[] getInput() {
                                Eater eater = eaters[eaterIndex];
                                int[] params = new int[4];
                                params[0] = eater.xCord;
                                params[1] = eater.yCord;
                                params[2] = cakeX;
                                params[3] = cakeY;
                                return params;
                            }
                        };

                        neuralNetworks[i] = nn;
                        if(nn.input.size()!=4){
                            eaters[eaterIndex].crashed=true;
                            GenomePrinter.printGenome(eval.genomes.get(eaterIndex), "/Users/eitankhemlin/Desktop/code/3in.png");
                        }

                    }
                }
            }
        }

    public class GameCanvas extends JPanel{
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Color.WHITE);
            if(debug){
                g.drawString(String.valueOf(debugEater.points), 10, 10);
                g.drawString(String.valueOf(cakeY), 50, 10);
                g.drawString(String.valueOf(cakeX), 100, 10);
                g.drawString(String.valueOf(debugEater.xCord), 750, 10);
                g.drawString(String.valueOf(debugEater.yCord), 800, 10);


                setForeground(Color.BLUE);
                g.fillOval(debugEater.xCord, debugEater.yCord, 2 * radius, 2 * radius);
                g.fillOval(cakeX, cakeY, 2 * cakeRadius, 2 * cakeRadius);
            }
            else{
                g.drawString(String.valueOf(genCount), 10, 10);
                g.drawString(String.valueOf(highestScore), 750, 10);

                g.fillOval(cakeX, cakeY, 2 * cakeRadius, 2 * cakeRadius);
                g.setColor(Color.blue);
                for(int i=0; i<populationSize; i++){
                    Eater eater = eaters[i];
                    if(eater.crashed) continue;
                    g.fillOval(eater.xCord, eater.yCord, 2 * radius, 2 * radius);
                    setForeground(Color.BLUE);
                }
            }


        }
    }



    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Main(); // Let the constructor do the job
            }
        });
    }
}