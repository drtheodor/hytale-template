{
  description = "Hytale Template";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    nixpkgs,
    flake-utils,
    ...
  }:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = nixpkgs.legacyPackages.${system};
        java = pkgs.zulu25;

        nativeBuildInputs = with pkgs; [];
        buildInputs = with pkgs; [
          java
          unzip
        ];
      in {
        devShells.default = pkgs.mkShell {
          inherit nativeBuildInputs buildInputs;

          shellHook = ''
            mkdir -p build/hytale
            if [ ! -f build/hytale/HytaleServer.jar ] || [ ! -f build/hytale/Assets.zip ]; then
              if [ -f ../HytaleServer.jar ] || [ -f ../Assets.zip ]; then
                ln -sf $(readlink -f ../HytaleServer.jar) build/hytale/HytaleServer.jar
                ln -sf $(readlink -f ../Assets.zip) build/hytale/Assets.zip
              else
                if [ ! -f build/hytale/hytale-downloader-linux-amd64 ]; then
                  curl --create-dirs -O --output-dir build/tmp/ https://downloader.hytale.com/hytale-downloader.zip
                  unzip build/tmp/hytale-downloader.zip -d build/hytale
                  rm build/tmp/hytale-downloader.zip
                fi

                ./build/hytale/hytale-downloader-linux-amd64
              fi
            fi
          '';

          env = {
            LD_LIBRARY_PATH = nixpkgs.lib.makeLibraryPath buildInputs;
            JAVA_HOME = "${java.home}";
          };
        };
      }
    );
}
