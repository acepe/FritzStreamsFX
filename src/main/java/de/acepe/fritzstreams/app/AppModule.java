package de.acepe.fritzstreams.app;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.inject.*;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;

import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.download.DownloadTask;
import de.acepe.fritzstreams.backend.stream.StreamInfo;
import de.acepe.fritzstreams.backend.vk.VKDownload;
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
        bind(String.class).annotatedWith(Names.named("APP_TITLE")).toInstance(APP_TITLE);
        bind(Settings.class).in(Singleton.class);
        bind(Player.class).in(Singleton.class);
        bind(ScreenManager.class).in(Singleton.class);
        bind(Dialogs.class).in(Singleton.class);
        bind(Application.class).toInstance(application);
        install(new FactoryModuleBuilder().implement(StreamInfo.class, StreamInfo.class)
                                          .build(StreamInfoFactory.class));
        install(new FactoryModuleBuilder().implement(DownloadTask.class, DownloadTask.class)
                                          .build(new TypeLiteral<DownloadTaskFactory<StreamInfo>>() {}));
        install(new FactoryModuleBuilder().implement(DownloadTask.class, DownloadTask.class)
                                          .build(new TypeLiteral<DownloadTaskFactory<VKDownload>>() {}));

        bind(HostServicesDelegate.class).toInstance(HostServicesFactory.getInstance(application));

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
