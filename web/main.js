// elements
const $inputs = {};
let $tables;

// functions
let display_balances, display_actors, display_cluster, display_transfers;

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
    };
}

function init_functions() {

    const shorten = (hash) => $("<div>").text(hash.substr(0, 30) + "…")
        .append(Gen.gen_cell_button("copy", () => {}));

    const prepare_transfer = (seed, index, address, balance) => {
        val("transfers_seed", seed);
        val("transfers_index", index);
        val("transfers_sender", address);
        val("transfers_receiver", "");
        val("transfers_remainder", "");
        val("transfers_value", balance);
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
        Gen.gen_cell_button("status", () => {})
    ];

    display_balances = Gen.gen_display_function('wallet', ["address", "balance", ""], wallet_serialize);
    display_actors = Gen.gen_display_function("actors", ["address", ""], actors_serialize);
    display_cluster = Gen.gen_display_function("cluster", ["address", "trust", ""], cluster_serialize);
    display_transfers = Gen.gen_display_function("transfers", ["transfer", ""], transfers_serialize);
}

/* ***** BUTTON ACTIONS ***** */

Btn.submit_transfer = () => {
    const seed = val("transfers_seed");
    const index = parseInt(val("transfers_index"));
    const receiver = val("transfers_receiver");
    const remainder = val("transfers_remainder");
    const value = val("transfers_value");
    submit_transfer(seed, index, receiver, remainder, value, alert);
}

Btn.create_actor = () => {
    const merkle_tree_depth = parseInt(val("merkle_tree_depth"));
    const seed = random_trytes(81);
    create_actor(seed, merkle_tree_depth, 0);
}

Btn.add_actor = () => {
    const address = val("cluster_address");
    const trust = parseFloat(val("cluster_trust"));
    add_actor(address, trust);
}

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

/* ***** GUI ***** */


/**
 * Loads and displays the addresses with their respective balances in the wallet section.
 * */
Gui.refresh_wallet = () => {
    get_balances(val("seed"), display_balances);
};

Gui.refresh_actors = () => {
    get_actors(display_actors);
};

Gui.refresh_cluster = () => {
    get_cluster(display_cluster)
};

Gui.refresh_transfers = () => {
    get_transfers(display_transfers)
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

function submit_transfer(seed, index, receiver, remainder, value, callback) {
    const request ={
        "action": "submit_transfer",
        "seed": seed,
        "index": index,
        "receiver": receiver,
        "remainder": remainder,
        "value": value
    };
    Api.ec_request(request, response => callback(response['hash']));
}

function get_transfers(callback) {
    Api.ec_request({"action": "get_transfers"}, response => callback(response['transfers']));
}

/**
 * @param seed {string} The seed to derive the addresses from.
 * @param {get_balances_callback} callback
 * */
function get_balances(seed, callback) {
    /**
     * @callback get_balances_callback
     * @param {json} balances map of the addresses derived from this seed with their respective balances in iotas
     */
    Api.ec_request({"action": "get_balances", "seed": seed}, response => callback(response['balances']));
}

/**
 * @param {get_actors_callback} callback
 * */
function get_actors(callback) {
    /**
     * @callback get_actors_callback
     * @param {array} addresses of all actors
     */
    Api.ec_request({"action": "get_actors"}, response => callback(response['actors']));
}

/**
 * @param {get_cluster_callback} callback
 * */
function get_cluster(callback) {
    /**
     * @callback get_cluster_callback
     * @param {array} list of actors (json object with string field 'address' and double field 'trust')
     */
    Api.ec_request({"action": "get_cluster"}, response => callback(response['cluster']));
}

function create_actor(seed, merkle_tree_depth, start_index) {
    Api.ec_request({"action": "create_actor", "seed": seed, "merkle_tree_depth": merkle_tree_depth, "start_index": start_index}, Gui.refresh_actors);
}

function add_actor(address, trust) {
    Api.ec_request({"action": "add_actor", "address": address, "trust": trust}, Gui.refresh_cluster);
}

/* ***** API ***** */

Api.ec_request = (request, success) => {
    Api.ajax("getModuleResponse", {"request": JSON.stringify(request), "path": "ec.ixi-1.0.jar"}, data => {
        const response = JSON.parse(data['response']);
        response['success'] ? (success ? success(response) : {}) : console.error("api error: " + response['error']);
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
            alert(JSON.stringify(error));
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