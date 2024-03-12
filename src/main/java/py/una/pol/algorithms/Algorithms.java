package py.una.pol.algorithms;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import py.una.pol.model.*;

import java.util.*;

public class Algorithms {

    /*
     * Algoritmo para establecer rutas utilizando RSA (Routing and Spectrum Assignment).
     *
     * @param demand: Objeto que representa la demanda de la red.
     * @param net: Grafo de la topología de la red.
     * @param kspaths: Lista de k caminos más cortos en el grafo.
     * @param fsMax: Número máximo de slots de frecuencia disponibles.
     * @param cores: Número de núcleos para la asignación de espectro.
     * @param capacity: Capacidad total del enlace.
     *
     * @return EstablisedRoute: Objeto que representa la ruta establecida con asignación de espectro.
     */
    public static EstablishedRoute findBestRoute(Demand demand, Graph<Integer, Link> network, List<GraphPath<Integer, Link>> shortestPaths, int cores, int capacity, int fsMax) {
        EstablishedRoute establishedRoute = null;
        int k = 0;

        try {
            while (k < shortestPaths.size() && shortestPaths.get(k) != null) {
                GraphPath<Integer, Link> ksp = shortestPaths.get(k);

                if (fsMax < demand.getFs()) {
                    fsMax = demand.getFs();
                }

                for (int i = 0; i <= fsMax - demand.getFs(); i++) {
                    List<Link> enlacesLibres = new ArrayList<>();
                    List<Integer> kspCores = new ArrayList<>();
                    BFR bestbfr = null;
                    double minBfrValue = Double.POSITIVE_INFINITY;
                    int minMsiIndex = Integer.MAX_VALUE;

                    for (Link link : ksp.getEdgeList()) {
                        List<BFR> listaBfr = new ArrayList<>();
                        for (int core = 0; core < cores; core++) {
                            // Calcular el índice de fin para evitar desbordamientos
                            int endIndex = Math.min(i + demand.getFs(), link.getCores().get(core).getFs().size());
                            List<FrequencySlot> bloqueFS = link.getCores().get(core).getFs().subList(i, i + endIndex);

                            //principio de continuidad
                            if (isFSBlockFree(bloqueFS)) {
                                BFR bfr = calculateBFRForCore(link.getCores().get(core).getFs());
                                listaBfr.add(bfr);

                                // Actualizar el mejor valor de BFR y MSI
                                if (bfr.getValue() < minBfrValue || (bfr.getValue() == minBfrValue && bfr.getMsi() < minMsiIndex)) {
                                    bestbfr = bfr;
                                    minBfrValue = bfr.getValue();
                                    minMsiIndex = bfr.getMsi();
                                } else if (bfr.getValue() == minBfrValue && bfr.getMsi() == minMsiIndex) {
                                    // Si hay más de un BFR óptimo con el mismo valor y MSI, elegir el que tenga el menor índice MSI
                                    bestbfr = bfr;
                                }

                                /* calcular bfr por nucleo */
                                /* calcular msi por nucleo */
                                /* calcular el crosstalk */
                                /* verificar contiguidad */

                            }
                        }


                    }

                    //isCrosstalkAware();

                }


                fsMax++;
                k++;

            }

            /* establecer ruta o retornar null */


        } catch (Exception e) {
            e.printStackTrace();
        }

        return establishedRoute;
    }

    private static Boolean isFSBlockFree(List<FrequencySlot> bloqueFS) {
        for (FrequencySlot fs : bloqueFS) {
            if (!fs.isFree()) {
                return false;
            }
        }
        return true;
    }

    private static Boolean isFSBlockContinuos() {


        return true;
    }

    public static BFR calculateBFRForCore(List<FrequencySlot> frequencySlotList) {
        BFR bfr = new BFR();

        double maxFreeBlockSize = 0; // Inicialmente no hay bloques libres
        double totalFreeSlots = 0; // Inicialmente no hay ranuras libres
        int maxOccupiedSlotIndex = -1; // Inicialmente no hay ranuras ocupadas

        int currentFreeBlockSize = 0; // Para rastrear el tamaño del bloque libre actual

        for (int i = 0; i < frequencySlotList.size(); i++) {
            FrequencySlot fs = frequencySlotList.get(i);
            if (fs.isFree()) {
                // Si la ranura de frecuencia está libre
                totalFreeSlots++; // Aumentar la cantidad total de ranuras libres
                currentFreeBlockSize++; // Aumentar el tamaño del bloque libre actual

                // Actualizar la cantidad máxima de bloques libres si es necesario
                maxFreeBlockSize = Math.max(maxFreeBlockSize, currentFreeBlockSize);
            } else {
                // Si la ranura de frecuencia está ocupada
                currentFreeBlockSize = 0;

                // Actualizar el índice del mayor slot ocupado si es necesario
                maxOccupiedSlotIndex = Math.max(maxOccupiedSlotIndex, i);
            }
        }

        // Asignar los valores calculados al objeto BFR
        bfr.setValue(1 - maxFreeBlockSize / totalFreeSlots);
        bfr.setMsi(maxOccupiedSlotIndex);

        return bfr;
    }



    public static boolean isCrosstalkAware(BFR bfr, Graph<Integer, Link> graph, Demand demand, int capacity) {
        // Implementa la lógica para verificar si el BFR cumple con la restricción de crosstalk aware
        // Puedes utilizar el BFR, el grafo, la demanda y la capacidad para realizar los cálculos necesarios.
        // Retorna true si el BFR cumple con la restricción, false en caso contrario.
        return true;
    }

    // Método para imprimir los caminos en kspPlaced
    private static void imprimirCaminos(List<GraphPath> kspPlaced) {
        for (int i = 0; i < kspPlaced.size(); i++) {
            GraphPath<Integer, DefaultWeightedEdge> path = kspPlaced.get(i);
            List<Integer> vertices = path.getVertexList();
            System.out.print("Camino " + (i + 1) + ": ");
            for (int j = 0; j < vertices.size(); j++) {
                System.out.print(vertices.get(j));
                if (j < vertices.size() - 1) {
                    System.out.print(" --> ");
                }
            }
            System.out.println();
        }
    }


}
