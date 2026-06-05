#!/bin/bash
# ============================================================
#  Script de build STICO - à exécuter sur ta machine locale
#  Prérequis : Java 21, Maven 3.x
# ============================================================
echo "=== Build STICO Plugin ==="

# Vérification Java
if ! command -v java &> /dev/null; then
    echo "❌ Java non trouvé. Installe Java 21+"
    exit 1
fi

# Vérification Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven non trouvé. Installe Maven 3.x"
    exit 1
fi

echo "✔ Java : $(java -version 2>&1 | head -1)"
echo "✔ Maven : $(mvn -version 2>&1 | head -1)"
echo ""
echo "🔨 Compilation en cours..."

mvn clean package -q

if [ $? -eq 0 ]; then
    echo "✅ Build réussi !"
    echo "📦 JAR disponible : target/STICO-1.0.0.jar"
    echo "👉 Copie STICO-1.0.0.jar dans le dossier plugins/ de ton serveur Paper 1.21"
else
    echo "❌ Build échoué. Lance 'mvn clean package' pour voir les erreurs."
fi
