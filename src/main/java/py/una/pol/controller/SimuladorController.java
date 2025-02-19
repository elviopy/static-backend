package py.una.pol.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.web.bind.annotation.*;
import py.una.pol.algorithms.Algorithms;
import py.una.pol.algorithms.ShortestPathFinder;
import py.una.pol.model.*;
import py.una.pol.utils.DemandSorter;
import py.una.pol.utils.DemandsGenerator;
import py.una.pol.utils.ResourceReader;
import py.una.pol.utils.Utils;

import java.io.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1")
@Api(value = "SimuladorController", description = "Operaciones relacionadas con la simulación de demandas")
public class SimuladorController {

    @PostMapping(path = "/simular")
    @ApiOperation(value = "Simula las demandas con las opciones proporcionadas")
    public Response simular(@ApiParam(value = "Opciones para la simulación de demandas", required = true) @RequestBody Options options) {
        List<EstablishedRoute> establishedRoutes = new ArrayList<>();
        List<GraphPath<Integer, Link>> kspaths = new ArrayList<>();
        List<Demand> demands;
        int demandsQ = 0, blocksQ = 0, fsMax = 0;

        //se crea la topologia con los parámetros seleccionados
        Graph<Integer, Link> net = createTopology(options.getTopology(), options.getCores(), options.getFsWidth(), options.getCapacity());

        //buscador de caminos mas cortos DIJKSTRA - KSP
        ShortestPathFinder shortestPathFinder = new ShortestPathFinder(net);

        //se generan aleatoriamente las demandas, de acuerdo a la cantidad proporcionadas por parámetro
        demands = DemandsGenerator.generateAndValidateDemands(options.getDemandsQuantity(), net, shortestPathFinder);

        // Ordenar en función del parámetro ascendente, descendente y aleatorio seria como viene
        if (options.getSortingDemands().equalsIgnoreCase("ASC")) {
            DemandSorter.sortByDistanceAscending(demands); // Orden ascendente (por defecto)
        } else if (options.getSortingDemands().equalsIgnoreCase("DESC")) {
            DemandSorter.sortByDistanceDescending(demands); // Orden descendente
        }

        for (Demand demand : demands) {
            System.out.println("-------PROCESANDO NUEVA DEMANDA----------");
            System.out.println("Demanda: " + demandsQ + ", Origen: " + demand.getSource() + ", Destino: " + demand.getDestination() + ", Cantidad de rutas en uso: " + establishedRoutes.size());
            demandsQ++;
            kspaths.clear();

            //se ejecuta k1 = Dijkstra, k3 = ksp (3 caminos), k5 = ksp (5 caminos)
            if (options.getShortestAlg().equals("k1")) {
                // Retorna el camino más corto de fuente a destino
                GraphPath<Integer, Link> shortestPath = shortestPathFinder.getShortestPath(demand.getSource(), demand.getDestination());
                // Agrega el camino a la lista kspaths
                kspaths.add(shortestPath);
            } else if (options.getShortestAlg().equals("k3")) {
                // Retorna los 3 caminos más cortos de fuente a destino
                List<GraphPath<Integer, Link>> kShortestPaths = shortestPathFinder.getKShortestPaths(demand.getSource(), demand.getDestination(), 3);
                for (GraphPath<Integer, Link> path : kShortestPaths) {
                    // Agrega cada camino a la lista kspaths
                    kspaths.add(path);
                }
            } else {
                // Retorna los 5 caminos más cortos de fuente a destino
                List<GraphPath<Integer, Link>> kShortestPaths = shortestPathFinder.getKShortestPaths(demand.getSource(), demand.getDestination(), 5);
                for (GraphPath<Integer, Link> path : kShortestPaths) {
                    // Agrega cada camino a la lista kspaths
                    kspaths.add(path);
                }
            }

            //busqueda de caminos disponibles, para establecer los enlaces
            EstablishedRoute establishedRoute = Algorithms.findBestRoute(demand, kspaths, options.getCores(), options.getCapacity(), fsMax, options.getMaxCrosstalk(), options.getCrosstalkPerUnitLenght());

            if (establishedRoute == null) {
                System.out.println("Demanda " + demandsQ + " BLOQUEADA ");
                demand.setBlocked(true);
                blocksQ++;

                break;

            } else {
                establishedRoutes.add(establishedRoute);
                Utils.assignFs(net, establishedRoute, options.getCrosstalkPerUnitLenght());
                System.out.println( "NUCLEO: " + establishedRoute.getPathCores() + ", FS: " + establishedRoute.getFs() + ", FsIndexBegin: " + establishedRoute.getFsIndexBegin());

                fsMax = Math.max(fsMax, establishedRoute.getFsMax());

            }
        }

        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("end", true);
        System.out.println("Resumen general del simulador");
        System.out.println("Cantidad de demandas: " + demandsQ);
        System.out.println("Cantidad de bloqueos: " + blocksQ);
        System.out.println("FSMAX: " + fsMax);
        System.out.println("Fin Simulación");

        Response response = new Response();
        response.setCantDemandas(demandsQ);
        response.setFsMax(fsMax);

        // Llama al método para escribir en el archivo CSV
        writeResponsesToCSV(options, fsMax);

        // Dibuja los FS utilizados y libres
        //printFSEntryStatus(net, options.getCores(), options.getCapacity());

        return response;
    }

    private int getCore(int limit, boolean[] tested) {
        Random r = new Random();
        int core = r.nextInt(limit);
        while (tested[core]) {
            core = r.nextInt(limit);
        }
        tested[core] = true;
        return core;
    }

    private Graph createTopology(String fileName, int numberOfCores, double fsWidh, int numberOffs) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Graph<Integer, Link> g = new SimpleWeightedGraph<>(Link.class);
            InputStream is = ResourceReader.getFileFromResourceAsStream(fileName);
            JsonNode object = objectMapper.readTree(is);

            //se agregan los vertices
            for (int i = 0; i < object.get("network").size(); i++) {
                g.addVertex(i);
            }
            int vertex = 0;
            for (JsonNode node : object.get("network")) {
                for (int i = 0; i < node.get("connections").size(); i++) {
                    int connection = node.get("connections").get(i).intValue();
                    int distance = node.get("distance").get(i).intValue();
                    List<Core> cores = new ArrayList<>();

                    for (int j = 0; j < numberOfCores; j++) {
                        Core core = new Core(fsWidh, numberOffs);
                        cores.add(core);
                    }

                    Link link = new Link(distance, cores, vertex, connection);
                    g.addEdge(vertex, connection, link);
                    g.setEdgeWeight(link, distance);
                }
                vertex++;
            }
            return g;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void writeResponsesToCSV(Options options, int fsMax) {
        String filePath = "src/main/resources/salida/salida.csv"; // Ruta del archivo CSV en la carpeta resources/salida
        //String filePath = "src\\main\\resources\\salida\\salida.csv"; //para windows


        // Verifica si la ruta es correcta y crea el archivo si no existe
        File file = new File(filePath);
        try {
            if (file.getParentFile() != null) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs(); // Crea los directorios necesarios
                }
            }
            if (file.createNewFile()) {
                System.out.println("El archivo ha sido creado: " + file.getAbsolutePath());
            } else {
                System.out.println("El archivo ya existe: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error al crear el archivo: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Escribir en el archivo
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            // Escribir encabezados si el archivo está vacío
            if (file.length() == 0) {
                writer.write("Topologia,KSP(caminos),Ordenamiento,fsMax");
                writer.newLine();
            }

            // Escribir datos de cada respuesta
            String line = options.getTopology() + "," + options.getShortestAlg() + "," + options.getSortingDemands() + "," + fsMax;
            writer.write(line);
            writer.newLine();

            System.out.println("Los datos se han guardado en el archivo CSV: " + filePath);
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void printFSEntryStatus(Graph<Integer, Link> net, int cores, int capacity) {
        for (Link link : net.edgeSet()) {
            System.out.println("Enlace DE: " + link.getFrom() + " A: " + link.getTo());
            for (int coreIndex = 0; coreIndex < cores; coreIndex++) {
                FrequencySlot[] coreSlots = link.getCores().get(coreIndex).getFs().toArray(new FrequencySlot[0]);
                System.out.print("Núcleo " + coreIndex + ": ");
                for (int fsIndex = 0; fsIndex < capacity; fsIndex++) {
                    System.out.print(coreSlots[fsIndex].isFree() ? "░" : "█");
                }
                System.out.println();
            }
        }
    }

}
