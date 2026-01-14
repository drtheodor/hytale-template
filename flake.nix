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
        ];
      in {
        devShells.default = pkgs.mkShell {
          inherit nativeBuildInputs buildInputs;

          env = {
            JAVA_HOME = "${java.home}";
          };
        };
      }
    );
}
