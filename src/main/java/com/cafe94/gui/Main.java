package com.cafe94.gui;

import com.cafe94.domain.Table;
import com.cafe94.domain.User;
import com.cafe94.enums.TableStatus;
import com.cafe94.persistence.*;
import com.cafe94.services.*;
import com.cafe94.util.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main extends Application {

    private static final Logger ROOT_LOGGER = Logger.getLogger("");
    private static final Logger LOGGER =
        Logger.getLogger(Main.class.getName());

    static {
        try {
            Handler[] handlers = ROOT_LOGGER.getHandlers();
            if (handlers.length > 0 &&
                handlers[0] instanceof ConsoleHandler) {
                ROOT_LOGGER.removeHandler(handlers[0]);
            }
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.FINE);
            consoleHandler.setFormatter(new SimpleFormatter());
            ROOT_LOGGER.addHandler(consoleHandler);
            ROOT_LOGGER.setLevel(Level.FINE);
        } catch (Exception e) {
            System.err.println("Error configuring logger: " +
                               e.getMessage());
        }
    }

    private IUserRepository userRepository;
    private ITableRepository tableRepository;
    private IMenuRepository menuRepository;
    private IOrderRepository orderRepository;
    private IBookingRepository bookingRepository;
    private PasswordHasher passwordHasher;
    private SessionManager sessionManager;
    private AuthorizationService authorizationService;
    private INotificationService notificationService;
    private IUserService userService;
    private IOrderService orderService;
    private IMenuService menuService;
    private IBookingService bookingService;
    private IReportingService reportingService;

    Stage primaryStage;

    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.log(Level.INFO, "init(): Initialising backend...");
        try {
            initialiseServices();
            LOGGER.log(Level.INFO,
                       "init(): Backend services initialised.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "FATAL: Failed backend init(). " +
                       "App cannot start.", e);
            throw e;
        }
    }

    private void initialiseServices() throws ClassNotFoundException {
        LOGGER.log(Level.INFO, "Starting component initialisation...");
        String dataDir = System.getProperty("cafe94.data.dir", "data");
        LOGGER.log(Level.CONFIG, "Using data directory: {0}", dataDir);
        String userDataFile = dataDir + File.separator + "users.dat";
        String tableDataFile = dataDir + File.separator + "tables.dat";
        String menuDataFile = dataDir + File.separator + "menu.dat";
        String orderDataFile = dataDir + File.separator + "orders.dat";
        String bookingDataFile = dataDir + File.separator + "bookings.dat";
        try {
            LOGGER.log(Level.CONFIG, "Instantiating Repositories...");
            List<Table> initialTables = createInitialTables();
            tableRepository = new TableRepository(tableDataFile,
                                                  initialTables);
            userRepository = new UserRepository(userDataFile);
            menuRepository = new MenuRepository(menuDataFile);
            orderRepository = new OrderRepository(orderDataFile);
            bookingRepository = new BookingRepository(bookingDataFile);
            LOGGER.log(Level.INFO, "Repositories instantiated.");
        } catch (Exception e) {
            throw new RuntimeException("Repo init failed", e);
        }
        try {
            LOGGER.log(Level.CONFIG, "Instantiating Utilities...");
            passwordHasher = new PlaceholderPasswordHasher();
            sessionManager = SessionManager.getInstance();
            Class.forName("com.cafe94.permission.PermissionLoader");
            authorizationService = new AuthorizationServiceImpl();
            notificationService = new ConsoleNotificationService();
            LOGGER.log(Level.INFO, "Utilities instantiated.");
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Util init failed", e);
        }
        try {
            LOGGER.log(Level.CONFIG, "Instantiating Services...");
            userService = new UserService(userRepository,
                authorizationService, passwordHasher, sessionManager);
            bookingService = new BookingService(bookingRepository,
                userRepository, tableRepository, authorizationService,
                notificationService);
            menuService = new MenuService(menuRepository,
                authorizationService);
            orderService = new OrderService(orderRepository,
                tableRepository, userRepository, authorizationService,
                notificationService);
            reportingService = new ReportingService(orderRepository,
                bookingRepository, userRepository, authorizationService);
            LOGGER.log(Level.INFO, "Services instantiated.");
        } catch (Exception e) {
            throw new RuntimeException("Service init failed", e);
        }
        LOGGER.log(Level.INFO, "Component initialization complete.");
    }

    private List<Table> createInitialTables() {
        List<Table> tables = new ArrayList<>();
        int tableId = 1;
        for (int i = 0; i < 4; i++) tables.add(
            new Table(tableId++, 2, TableStatus.AVAILABLE));
        for (int i = 0; i < 4; i++) tables.add(
            new Table(tableId++, 4, TableStatus.AVAILABLE));
        for (int i = 0; i < 2; i++) tables.add(
            new Table(tableId++, 8, TableStatus.AVAILABLE));
        tables.add(new Table(tableId++, 10, TableStatus.AVAILABLE));
        LOGGER.log(Level.CONFIG, "Gen {0} initial tables.", tables.size());
        return tables;
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        LOGGER.log(Level.INFO,
                   "start(): Starting JavaFX application UI...");
        try {
            loadScene("/com/cafe94/gui/ProfileChoiceScreen.fxml");
            primaryStage.setTitle("Cafe94 - Welcome");
            primaryStage.setOnCloseRequest(event ->
                LOGGER.log(Level.INFO, "App window closing.")
            );
            primaryStage.show();
            LOGGER.log(Level.INFO, "start(): Primary stage shown.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed load initial FXML/UI.", e);
            showErrorDialog("UI Load Error", "Could not load UI.\n" +
                            e.getMessage());
            throw new RuntimeException("Failed start UI", e);
        }
    }

    @Override
    public void stop() throws Exception {
        LOGGER.log(Level.INFO, "stop(): JavaFX application stopping.");
        super.stop();
    }

    public void loadScene(String fxmlPath) throws IOException {
        FXMLLoader loader = createLoader(fxmlPath);
        Parent root = loader.load();
        injectDependencies(loader.getController());
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        LOGGER.info("Loaded scene: " + fxmlPath);
    }

    public Object openWindow(String fxmlPath, String title, boolean modal)
            throws IOException {
        FXMLLoader loader = createLoader(fxmlPath);
        Parent root = loader.load();
        Object controller = loader.getController();
        injectDependencies(controller);
        Stage newStage = new Stage();
        newStage.setTitle(title);
        newStage.setScene(new Scene(root));
        if (modal) {
            newStage.initModality(Modality.APPLICATION_MODAL);
            newStage.initOwner(primaryStage);
        }
        newStage.show();
        LOGGER.log(Level.INFO,
        "Opened window: {0} Title: {1}", new Object[]{fxmlPath, title});
        return controller;
    }

    FXMLLoader createLoader(String fxmlPath) throws IOException {
        URL fxmlUrl = getClass().getResource(fxmlPath);
        if (fxmlUrl == null) {
            LOGGER.log(Level.SEVERE,
            "Cannot find FXML resource: {0}", fxmlPath);
            throw new IOException("Cannot load FXML: " + fxmlPath);
        }
        LOGGER.log(Level.CONFIG, "Loading FXML from: {0}", fxmlUrl);
        return new FXMLLoader(fxmlUrl);
    }

    void injectDependencies(Object controller) {
        LOGGER.log(Level.FINE, "Injecting dependencies into: {0}",
        controller.getClass().getName());
        User currentUser = sessionManager.getCurrentUser();

        if (controller instanceof NeedsMainApp) {
             ((NeedsMainApp) controller).setMainApp(this);
        }
        if (controller instanceof NeedsSessionManager) {
             ((NeedsSessionManager) controller)
             .setSessionManager(sessionManager);
        }

        if (controller instanceof LoginController) {
            LoginController c = (LoginController) controller;
            c.setUserService(userService);
            c.setOrderService(orderService);
            c.setMenuService(menuService);
            c.setBookingService(bookingService);
            c.setReportingService(reportingService);
            c.setUserRepository(userRepository);
            c.setTableRepository(tableRepository);
            c.setBookingRepository(bookingRepository);
        } else if (controller instanceof StaffManagementScreen) {
            StaffManagementScreen c = (StaffManagementScreen) controller;
            c.setUserService(userService);
            c.setUserRepository(userRepository);
             if (currentUser != null) c.setCurrentUser(currentUser);
             else LOGGER.warning("No user for StaffManagementScreen");
        } else if (controller instanceof BookingApproverScreen) {
            BookingApproverScreen c = (BookingApproverScreen) controller;
            c.setBookingService(bookingService);
             if (currentUser != null) c.setCurrentUser(currentUser);
             else LOGGER.warning("No user for BookingApproverScreen");
        } else if (controller instanceof OutstandingOrdersScreen) {
            OutstandingOrdersScreen c = (OutstandingOrdersScreen) controller;
            c.setOrderService(orderService);
             if (currentUser != null) c.setCurrentUser(currentUser);
             else LOGGER.warning("No user for OutstandingOrdersScreen");
        } else if (controller instanceof DriverDeliveriesScreen) {
            DriverDeliveriesScreen c = (DriverDeliveriesScreen) controller;
            c.setOrderService(orderService);
             if (currentUser != null) c.setCurrentUser(currentUser);
             else LOGGER.warning("No user for DriverDeliveriesScreen");
        } else if (controller instanceof BookingRequestScreen) {
             BookingRequestScreen c = (BookingRequestScreen) controller;
             c.setBookingService(bookingService);
             if (currentUser != null) c.setCurrentUser(currentUser);
             else LOGGER.warning("No user for BookingRequestScreen");
        } else if (controller instanceof CancelBookingScreen) {
            CancelBookingScreen c = (CancelBookingScreen) controller;
            c.setBookingService(bookingService);
            if (currentUser != null) c.setCurrentUser(currentUser);
            else LOGGER.warning("No user for CancelBookingScreen");
        } else if (controller instanceof DeliveryApproverScreen) {
            DeliveryApproverScreen c = (DeliveryApproverScreen) controller;
            c.setOrderService(orderService);
            c.setUserService(userService);
            c.setUserRepository(userRepository);
            if (currentUser != null) c.setCurrentUser(currentUser);
            else LOGGER.warning("No user for DeliveryApproverScreen");
        } else if (controller instanceof BookingHistoryScreen) {
            BookingHistoryScreen c = (BookingHistoryScreen) controller;
            c.setBookingService(bookingService);
        } else if (controller instanceof EatInOrderScreen) {
            EatInOrderScreen c = (EatInOrderScreen) controller;
            c.setMenuService(menuService);
            c.setOrderService(orderService);
            c.setTableRepository(tableRepository);
            if (currentUser != null) c.setCurrentUser(currentUser);
            else LOGGER.warning("No user for EatInOrderScreen");
        } else if (controller instanceof MenuViewScreen) {
            MenuViewScreen c = (MenuViewScreen) controller;
            c.setMenuService(menuService);
        } else if (controller instanceof OrderHistoryScreen) {
             OrderHistoryScreen c = (OrderHistoryScreen) controller;
             c.setOrderService(orderService);
        } else if (controller instanceof ProfileChoiceScreen) {
        } else if (controller instanceof ProfileSelectionScreen) {
        } else if (controller instanceof ReportsScreen) {
            ReportsScreen c = (ReportsScreen) controller;
            c.setReportingService(reportingService);
            if (currentUser != null) c.setCurrentUser(currentUser);
            else LOGGER.warning("No user for ReportsScreen");
        } else if (controller instanceof SetDailySpecialScreen) {
            SetDailySpecialScreen c = (SetDailySpecialScreen) controller;
            c.setMenuService(menuService);
             if (currentUser != null) c.setCurrentUser(currentUser);
             else LOGGER.warning("No user for SetDailySpecialScreen");
        } else if (controller instanceof TakeawayOrderScreen) {
            TakeawayOrderScreen c = (TakeawayOrderScreen) controller;
            c.setMenuService(menuService);
            c.setOrderService(orderService);
            if (currentUser != null) c.setCurrentUser(currentUser);
            else LOGGER.warning("No user for TakeawayOrderScreen");
        } else if (controller instanceof ViewCustomersScreen) {
            ViewCustomersScreen c = (ViewCustomersScreen) controller;
            c.setUserRepository(userRepository);
            c.setOrderService(orderService);
             if (currentUser != null) c.setCurrentUser(currentUser);
             else LOGGER.warning("No user for ViewCustomersScreen");
        } else if (controller instanceof CustomerSignUpScreen) {
            CustomerSignUpScreen c = (CustomerSignUpScreen) controller;
            c.setUserService(userService);
        } else if (controller instanceof CustomerLoginScreen) {
            CustomerLoginScreen c = (CustomerLoginScreen) controller;
            c.setUserService(userService);
        } else if (controller instanceof CustomerActionScreen) {
        }
        else {
            LOGGER.log(Level.FINER,
            "No specific DI configured for type: {0}",
            controller.getClass().getName());
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public interface NeedsMainApp {
        void setMainApp(Main mainApp);
    }
    public interface NeedsSessionManager {
        void setSessionManager(SessionManager manager);
    }
}