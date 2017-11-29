package de.acepe.fritzstreams;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.function.Supplier;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.assistedinject.FactoryProvider;

import de.acepe.fritzstreams.backend.Player;
import de.acepe.fritzstreams.backend.Settings;
import de.acepe.fritzstreams.backend.stream.StreamInfo;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import okhttp3.OkHttpClient;

public class AppModule extends AbstractModule {

    private final Application application;
    private final Supplier<Injector> injectorSupplier;

    public AppModule(Application application, Supplier<Injector> injectorSupplier) {
        this.application = application;
        this.injectorSupplier = injectorSupplier;
    }

    @Override
    protected void configure() {
        bind(Settings.class).in(Singleton.class);
        bind(Player.class).in(Singleton.class);
        bind(ScreenManager.class).in(Singleton.class);
        bind(Application.class).toInstance(application);
        install(new FactoryModuleBuilder()
                .implement(StreamInfo.class, StreamInfo.class)
                .build(StreamInfoFactory.class));
    }

    @Provides
    @Singleton
    public OkHttpClient createHttpClient() {
        return new OkHttpClient().newBuilder().connectTimeout(5, SECONDS).readTimeout(10, SECONDS).build();
    }

    @Provides
    public FXMLLoader createFXMLLoader() {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setControllerFactory(injectorSupplier.get()::getInstance);
        return fxmlLoader;
    }

    // @Provides
    // public StreamController createStreamController(ScreenManager screenManager) {
    // return screenManager.loadFragment(Fragments.STREAM);
    // }

}
