const $inputs = {};
let $tables;

// namespaces
const Gui = {};
const Gen = {};
const Api = {};
const Btn = {};

window.onload = function () {
    init_elements();
    init_functions();
    Gui.refresh_wallet();
    Gui.refresh_actors();
    Gui.refresh_cluster();
    Gui.refresh_transfers();
};

function init_elements() {

    $('input').each((_, $el) => { $inputs[$el.id] = $($el); });
    val("seed", random_trytes(81));
    val("merkle_tree_depth",3);
    val("cluster_trust", 0.2);

    $tables = {
        "wallet": $('#wallet table'),
        "cluster": $('#cluster table'),
        "actors": $('#actors table'),
        "transfers": $('#transfers table'),
        "transactions": $('#transactions table'),
        "confidences": $('#confidences table'),
    };
}

function init_functions() {

    const shorten = (hash) => $("<div>").text(hash.substr(0, 30) + "…")
        .append(Gen.gen_cell_button("copy", () => {copy_to_clipboard(hash)}));

    const prepare_transfer = (seed, index, address, balance) => {
        val("transfers_seed", seed);
        val("transfers_index", index);
        val("transfers_sender", address);
        val("transfers_receiver", "");
        val("transfers_remainder", "");
        val("transfers_value", balance);
        Gui.show("new_transfer")
    };

    const wallet_serialize = (entry, index) => [
        shorten(entry['address']),
        entry['balance'],
        $("<div>")
            .append(Gen.gen_cell_button("send", () => {prepare_transfer(val("seed"), index, entry['address'], entry['balance'])}))
    ];

    const actors_serialize = actor => [
        shorten(actor),
        Gen.gen_cell_button("✘", () => {})
    ];

    const cluster_serialize = entry => [
        shorten(entry['address']),
        entry['trust'],
        Gen.gen_cell_button("✘", () => {})
    ];

    const transfers_serialize = hash => [
        shorten(hash),
        Gen.gen_cell_button("status", () => { Gui.load_transactions(hash) })
    ];

    const transactions_serialize = entry => [
        shorten(entry['hash']),
        shorten(entry['address']),
        entry['value'],
        $("<div>").text(entry['confidence']).append(Gen.gen_cell_button("details", () => { Gui.show("confidences"); Gui.display_confidences(entry['confidences']) }))
    ];

    const confidences_serialize = entry => [
        shorten(entry['actor']),
        entry['confidence']
    ];

    Gui.display_balances = Gen.gen_display_function('wallet', ["address", "balance", ""], wallet_serialize);
    Gui.display_actors = Gen.gen_display_function("actors", ["address", ""], actors_serialize);
    Gui.display_cluster = Gen.gen_display_function("cluster", ["address", "trust", ""], cluster_serialize);
    Gui.display_transfers = Gen.gen_display_function("transfers", ["transfer", ""], transfers_serialize);
    Gui.display_transactions = Gen.gen_display_function("transactions", ["hash", "address", "value", "confidence"], transactions_serialize);
    Gui.display_confidences = Gen.gen_display_function("confidences", ["actor", "confidence"], confidences_serialize);
}

/* ***** BUTTON ACTIONS ***** */

Btn.submit_transfer = () => {
    if(!Gui.validate_form('new_transfer'))
        return;
    const seed = val("transfers_seed");
    const index = parseInt(val("transfers_index"));
    const receiver = val("transfers_receiver");
    const remainder = val("transfers_remainder");
    const value = val("transfers_value");
    Api.submit_transfer(seed, index, receiver, remainder, value, () => { Gui.refresh_transfers(); Gui.hide("new_transfer") });
};

Btn.create_actor = () => {
    const merkle_tree_depth = parseInt(val("merkle_tree_depth"));
    const seed = random_trytes(81);
    Api.create_actor(seed, merkle_tree_depth, 0);
};

Btn.add_actor = () => {
    const address = val("cluster_address");
    const trust = parseFloat(val("cluster_trust"));
    Api.add_actor(address, trust);
};

/* ***** HELPERS ***** */

function val(input, val = null) {
    return val === null ? $inputs[input].val() : $inputs[input].val(val);
}

function random_trytes(length) {
    let seed = "";
    for(let i = 0; i < length; i++)
        seed += random_tryte();
    return seed;
}

function random_tryte() {
    const TRYTES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ9";
    return TRYTES.charAt(Math.floor(Math.random()*27));
}

function copy_to_clipboard(message) {
    let input = document.createElement('input');
    input.value = message;
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
}

/* ***** GUI ***** */


/**
 * Loads and displays the addresses with their respective balances in the wallet section.
 * */
Gui.refresh_wallet = () => {
    Api.get_balances(val("seed"), Gui.display_balances);
};

Gui.refresh_actors = () => {
    Api.get_actors(Gui.display_actors);
};

Gui.refresh_cluster = () => {
    Api.get_cluster(Gui.display_cluster)
};

Gui.refresh_transfers = () => {
    Api.get_transfers(Gui.display_transfers)
};

Gui.load_transactions = (bundle_head) => {
    Api.get_transactions(bundle_head, (transactions) => { Gui.display_transactions(transactions); Gui.show("transactions"); });
};

Gui.handle_error = function (message) {
    swal("Whoops!", message, "error");
    console.log(message);
};

Gui.hide = (id) => { $('#'+id).addClass("hidden"); };
Gui.show = (id) => { $('#'+id).removeClass("hidden"); };

Gui.validate_form = (id) => {
    const $form_inputs = $('#'+id+" input");
    for(let i = 0; i < $form_inputs.length; i++) {
        const $el = $($form_inputs[i]);
        const pattern = $el.attr("pattern");
        const regex = new RegExp(pattern);
        if(pattern && !$el.val().match(regex)) {
            $el.addClass("invalid");
            Gui.handle_error("field '" + $el.attr("id") + "' does not match expected pattern: " + pattern);
            return false;
        }
        $el.removeClass("invalid");
    }

    return true;
};

/* ***** GENERATORS ***** */

Gen.gen_display_function = function (table_name, table_head, object_serializer) {
    let $table = $tables[table_name];
    return function(objects) {
        $table.html(Gen.gen_table_row(table_head, true));
        $.each(objects, function (index, object) {
            $table.append(Gen.gen_table_row(object_serializer(object, index)));
        });
    };
};

/**
 * @param cells array of row's cell contents in correct order
 * @param th if enabled will use <th> cells instead of <td>
 * @return table row whose cells are filled with respective content
 * */
Gen.gen_table_row = function (cells, th = false) {
    const $tr = $("<tr>");
    $.each(cells, function (_, cell) {
        $tr.append($(th ? "<th>" : "<td>").html(cell));
    });
    return $tr;
};

Gen.gen_cell_button = (name, onclick) => $("<input>").attr("type", "button").val(name).click(onclick);

/* ***** API WRAPPERS ***** */

Api.submit_transfer = function (seed, index, receiver, remainder, value, callback) {
    const request ={
        "action": "submit_transfer",
        "seed": seed,
        "index": index,
        "receiver": receiver,
        "remainder": remainder,
        "value": value,
        "check_balances": false
    };
    Api.ec_request(request, response => callback(response['hash']));
};

Api.get_transfers = function (callback) {
    Api.ec_request({"action": "get_transfers"}, response => callback(response['transfers']));
};

Api.get_transactions = function (bundle_head, callback) {
    Api.ec_request({"action": "get_transactions", "bundle_head": bundle_head}, response => callback(response['transactions']));
};

Api.get_balances = function (seed, callback) {
    Api.ec_request({"action": "get_balances", "seed": seed}, response => callback(response['balances']));
};

Api.get_actors = function (callback) {
    Api.ec_request({"action": "get_actors"}, response => callback(response['actors']));
};

Api.get_cluster = function (callback) {
    Api.ec_request({"action": "get_cluster"}, response => callback(response['cluster']));
};

Api.create_actor = function (seed, merkle_tree_depth, start_index) {
    Api.ec_request({"action": "create_actor", "seed": seed, "merkle_tree_depth": merkle_tree_depth, "start_index": start_index}, Gui.refresh_actors);
};

Api.add_actor = function (address, trust) {
    Api.ec_request({"action": "add_actor", "address": address, "trust": trust}, Gui.refresh_cluster);
};

/* ***** API ***** */

Api.ec_request = (request, success) => {
    Api.ajax("getModuleResponse", {"request": JSON.stringify(request), "path": "ec.ixi-1.0.jar"}, data => {
        const response = JSON.parse(data['response']);
        response['success'] ? (success ? success(response) : {}) : Gui.handle_error("api error: " + response['error']);
    });
};

Api.ajax = (path, data, success) => {
    data['password'] = "change_me_now";
    $.ajax({
        url: "http://localhost:2187/" + path,
        method: "POST",
        data: Api.serialize_post_data(data),
        dataType: "json",
        success: success,
        error: function (error) {
            Gui.handle_error(JSON.stringify(error));
        }
    });
};

Api.serialize_post_data = (data) => {
    const serialized = [];
    $.each(data, function (name, value) {
        serialized.push({"name": name, "value": value});
    });
    return serialized;
};