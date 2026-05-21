package es.yaroki.educhronos.solver.cli;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;

public class HelloOrTools {
    public static void main(String[] args) {
        Loader.loadNativeLibraries();
        System.out.println("OR-Tools nativos cargados.");

        CpModel model = new CpModel();
        IntVar x = model.newIntVar(0, 10, "x");
        IntVar y = model.newIntVar(0, 10, "y");
        model.addLessThan(x, y);

        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("x = " + solver.value(x));
            System.out.println("y = " + solver.value(y));
        } else {
            System.out.println("Sin solución: " + status);
            System.exit(1);
        }
    }
}
