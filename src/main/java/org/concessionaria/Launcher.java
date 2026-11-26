package org.concessionaria;

/**
 * Ponto de entrada alternativo. Algumas IDEs/empacotadores falham ao iniciar uma classe que
 * estende Application diretamente sem módulos; este launcher contorna o problema.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
