package de.acepe.fritzstreams.app;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.StreamManager;
import de.acepe.fritzstreams.ui.Dialogs;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

public class AppModule extends AbstractModule {

    private static final String APP_TITLE = "Fritz Streams";
    private final Application application;
    private final Supplier<Injector> injectorSupplier;

    public AppModule(Application application, Supplier<Injector> injectorSupplier) {
        this.application = application;
        this.injectorSupplier = injectorSupplier;
    }

    @Override
    protected void configure() {
        bind(Application.class).toInstance(application);
        bind(String.class).annotatedWith(Names.named("APP_TITLE")).toInstance(APP_TITLE);

        bind(Settings.class).in(Singleton.class);
        bind(Player.class).in(Singleton.class);
        bind(ScreenManager.class).in(Singleton.class);
        bind(StreamManager.class).in(Singleton.class);
        bind(Dialogs.class).in(Singleton.class);

        FactoryModuleBuilder builder = new FactoryModuleBuilder();
        install(builder.build(OnDemandStreamFactory.class));
        install(builder.build(StreamCrawlerFactory.class));
        install(builder.build(DownloadTaskFactory.class));

    }

    @Provides
    @Singleton
    public OkHttpClient createHttpClient() {
        int maxConnections = 10;
        int keepAliveDuration = 15;
        ConnectionPool pool = new ConnectionPool(maxConnections, keepAliveDuration, TimeUnit.SECONDS);

        return new OkHttpClient().newBuilder()
                                 .connectionPool(pool)
                                 .connectTimeout(5, SECONDS)
                                 .readTimeout(10, SECONDS)
                                 .build();
    }

    @Provides
    public FXMLLoader createFXMLLoader() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setControllerFactory(injectorSupplier.get()::getInstance);
        return fxmlLoader;
    }

}
