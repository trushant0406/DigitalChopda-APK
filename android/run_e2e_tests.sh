#!/usr/bin/env bash
set -e

# Add Maestro to PATH
export PATH="$HOME/.maestro/bin:$PATH"

echo "=========================================================="
echo " 🚀 DigitalChopda Local E2E Engine (Powered by Maestro)"
echo "=========================================================="

if ! command -v maestro &> /dev/null; then
    echo "❌ Maestro CLI not found. Please install it with:"
    echo "   curl -fsSL 'https://get.maestro.mobile.dev' | bash"
    exit 1
fi

echo "✓ Maestro CLI: $(maestro --version)"

# Check connected ADB devices
echo "📱 Checking connected Android devices via ADB..."
DEVICES=$(adb devices | grep -v "List" | grep "device" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "⚠️ No ADB devices detected. Please start an emulator or connect a device over USB/Wi-Fi."
    exit 1
fi

echo "✓ Found device(s):"
echo "$DEVICES"

# Determine target device flag (optional override)
DEVICE_FLAG=""
if [ -n "$DEVICE" ]; then
    DEVICE_FLAG="--device $DEVICE"
    echo "🎯 Targeting explicit device: $DEVICE"
else
    echo "🎯 Using active connected device automatically"
fi

# Build APK if needed
if [ "$1" == "--build" ]; then
    echo "🔨 Building DigitalChopda Debug APK..."
    ./gradlew assembleDebug
fi

# Run the 4 core flows
echo ""
echo "▶ Running Flow 1: Authentication & Dashboard..."
maestro test $DEVICE_FLAG .maestro/01_login_flow.yaml

echo ""
echo "▶ Running Flow 2: Add Customer (Khata Master)..."
maestro test $DEVICE_FLAG .maestro/02_add_customer_flow.yaml

echo ""
echo "▶ Running Flow 3: Record Payment & Passbook Verification..."
maestro test $DEVICE_FLAG .maestro/03_record_transaction_flow.yaml

echo ""
echo "▶ Running Flow 4: Billing Hub -> Party Dropdown -> Itemized Products & Pricing..."
maestro test $DEVICE_FLAG .maestro/04_billing_and_itemized_detail_flow.yaml

echo ""
echo "▶ Running Flow 5: Party Detail -> + New Bill Preselect Flow..."
maestro test $DEVICE_FLAG .maestro/05_party_new_bill_preselect_flow.yaml

echo ""
echo "▶ Running Flow 6: Product Catalog & Fast Add Flow..."
maestro test $DEVICE_FLAG .maestro/06_product_creation_and_search_flow.yaml

echo ""
echo "▶ Running Flow 7: POS Fast Cart, Inline Search & Quantity Stepper Flow..."
maestro test $DEVICE_FLAG .maestro/07_pos_fast_cart_and_stepper_flow.yaml

echo ""
echo "▶ Running Flow 8: Outbox Sync Transparency & Retail Soundbox Flow..."
maestro test $DEVICE_FLAG .maestro/08_sync_transparency_and_soundbox_flow.yaml

echo ""
echo "▶ Running Flow 9: Statement Download & PDF Flow..."
maestro test $DEVICE_FLAG .maestro/09_statement_download_flow.yaml

# Copy screenshots to artifact directory and repo
mkdir -p .maestro/screenshots
cp -f $(find "$HOME/.maestro/tests" -name "*.png" -mmin -10 2>/dev/null) .maestro/screenshots/ 2>/dev/null || true

echo ""
echo "=========================================================="
echo " 🎉 ALL 9 CORE E2E FLOWS COMPLETED SUCCESSFULLY!"
echo " Screenshots saved in android/.maestro/screenshots/"
echo "=========================================================="
